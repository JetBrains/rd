package com.jetbrains.rd.util.test.cases

import com.jetbrains.rd.util.spinUntil
import com.jetbrains.rd.util.threading.ByteBufferAsyncProcessor
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.time.Duration
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.CRC32
import kotlin.concurrent.thread
import kotlin.test.assertFalse

class ByteBufferAsyncProcessorTest {

    fun InputStream.readInt32() : Int? {
        val b1 = read()
        val b2 = read()
        val b3 = read()
        val b4 = read()
        val res = b1 or (b2 shl 8) or (b3 shl 16) or (b4 shl 24)
        if (res < 0)
            return null

        return res
    }

    fun Int.toByteArray() = byteArrayOf(
            (this ushr 0 and 0xff).toByte(),
            (this ushr 8 and 0xff).toByte(),
            (this ushr 16 and 0xff).toByte(),
            (this ushr 24 and 0xff).toByte()
            )

    @Test
    fun testOneProducer() {
        var prev = -1
        val buffer = ByteBufferAsyncProcessor("TestAsyncProcessor", 4) {
            assert(it.ptr > 0)
            val x = ByteArrayInputStream(it.data).readInt32()!!
            assert (x > prev)
            prev = x
        }

        buffer.start()
        repeat(1000) {
            buffer.put(it.toByteArray())
        }
    }

    // RIDER-127935: ByteBufferAsyncProcessor's public API is all `synchronized` and only
    // clear/reprocessUnacknowledged forbid the processing thread — i.e. the class implies put()/acknowledge()
    // are callable from any thread. This drives it exactly the way SocketWire does (many producers calling
    // put(), one consumer/processor draining + assigning seqn + async acks) and verifies the fundamental
    // contract: the bytes handed to the processor, concatenated in invocation order, must reconstruct the
    // put() frames faithfully. RED: under concurrent producers the ring hands out chunks out of fill order,
    // permuting the byte stream.
    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    fun concurrentProducersPreserveStream() = runStreamPreservationTest(producers = 16)

    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    fun twoProducersPreserveStream() = runStreamPreservationTest(producers = 2)

    // Control: identical total volume from a single producer must stay GREEN, proving the failure is
    // concurrency-specific (not the harness / codec / chunking).
    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    fun oneProducerLargeFramesPreserveStream() = runStreamPreservationTest(producers = 1)

    private fun runStreamPreservationTest(producers: Int) {
        val rounds = 15
        val iterations = 20
        val frameSize = 128_000   // > chunkSize -> every frame spans several chunks
        val chunkSize = 16370     // ByteBufferAsyncProcessor default

        val corruptions = ArrayList<String>()                  // written only by the single processor thread
        val verifier = StreamVerifier(frameSize, corruptions)  // re-frames the concatenated chunk stream
        val bytesProcessed = AtomicLong(0)
        var sentSeqn = 0L                                       // touched only by the processor thread
        val ackExecutor = Executors.newSingleThreadExecutor()

        lateinit var buffer: ByteBufferAsyncProcessor
        buffer = ByteBufferAsyncProcessor("TestConcurrentProducers", chunkSize) { chunk ->
            if (chunk.isNotProcessed) chunk.seqn = ++sentSeqn   // mimic SocketWire.send0
            verifier.feed(chunk.data, chunk.ptr)                // mimic the receiver reassembling the stream
            bytesProcessed.addAndGet(chunk.ptr.toLong())        // AFTER feed -> happens-before the final read
            val seqn = chunk.seqn
            ackExecutor.execute { buffer.acknowledge(seqn) }    // mimic the counterpart ack -> ring reuse/lap
        }
        buffer.start()

        var bytesPutTotal = 0L
        var drained = true
        for (round in 0 until rounds) {
            if (corruptions.isNotEmpty()) break
            val base = round * iterations
            val barrier = CyclicBarrier(producers)
            val threads = (0 until producers).map { tid ->
                thread(isDaemon = true, name = "producer-$round-$tid") {
                    barrier.await()
                    for (it in 0 until iterations)
                        buffer.put(makeFrame(tid, base + it, frameSize))
                }
            }
            threads.forEach {
                it.join(60_000)
                assertFalse(it.isAlive, "Thread ${it.name} should be terminated.")
            }
            bytesPutTotal += producers.toLong() * iterations * frameSize
            drained = spinUntil(30_000) { corruptions.isNotEmpty() || bytesProcessed.get() >= bytesPutTotal }
            if (!drained) break
        }

        buffer.terminate(Duration.ofSeconds(30))
        ackExecutor.shutdownNow()

        val expectedFrames = producers * rounds * iterations
        assertTrue(corruptions.isEmpty(),
            "ByteBufferAsyncProcessor corrupted the stream under $producers producer(s): ${corruptions.take(5)}")
        assertTrue(drained,
            "timed out draining: bytesProcessed=${bytesProcessed.get()} expected=$bytesPutTotal")
        assertTrue(verifier.frameCount == expectedFrames,
            "frame count ${verifier.frameCount} != expected $expectedFrames")
    }
}

// --- RIDER-127935 stream-preservation harness (socket-free) ---------------------------------------------

/** Self-verifying frame: [len:int32][producerId:int32][seq:int32][crc32:int32][filler…], big-endian. */
private fun makeFrame(producerId: Int, seq: Int, size: Int): ByteArray {
    val b = ByteArray(size)
    val seed = producerId * 1_000_003 + seq
    for (k in 16 until size) b[k] = ((seed + k) and 0xFF).toByte()
    putIntBE(b, 0, size)
    putIntBE(b, 4, producerId)
    putIntBE(b, 8, seq)
    putIntBE(b, 12, crc32(b, 16, size - 16))
    return b
}

/**
 * Re-frames the byte stream produced by concatenating chunk data in processor-invocation order. Holds only a
 * single-frame buffer (no full accumulation), so memory stays bounded regardless of total volume. A
 * chunk-level reorder splits a frame and trips the very next len/crc check.
 */
private class StreamVerifier(private val frameSize: Int, private val corruptions: MutableList<String>) {
    private val frame = ByteArray(frameSize)
    private var have = 0
    private val seen = HashSet<Long>()
    var frameCount = 0
        private set

    fun feed(data: ByteArray, len: Int) {
        var pos = 0
        while (pos < len) {
            val take = minOf(frameSize - have, len - pos)
            System.arraycopy(data, pos, frame, have, take)
            have += take
            pos += take
            if (have == frameSize) {
                verifyFrame()
                have = 0
            }
        }
    }

    private fun verifyFrame() {
        if (corruptions.size >= 8) return // desynced stream produces noise; the first few anomalies are enough
        val len = getIntBE(frame, 0)
        if (len != frameSize) {
            corruptions.add("bad len=$len at frame #$frameCount (expected $frameSize)")
            return
        }
        val pid = getIntBE(frame, 4)
        val seq = getIntBE(frame, 8)
        val storedCrc = getIntBE(frame, 12)
        val actualCrc = crc32(frame, 16, frameSize - 16)
        if (storedCrc != actualCrc) {
            corruptions.add("crc mismatch frame #$frameCount pid=$pid seq=$seq stored=$storedCrc actual=$actualCrc")
            return
        }
        val key = (pid.toLong() shl 32) or (seq.toLong() and 0xFFFFFFFFL)
        if (!seen.add(key)) corruptions.add("duplicate frame pid=$pid seq=$seq at #$frameCount")
        frameCount++
    }
}

private fun putIntBE(b: ByteArray, off: Int, v: Int) {
    b[off] = (v ushr 24).toByte()
    b[off + 1] = (v ushr 16).toByte()
    b[off + 2] = (v ushr 8).toByte()
    b[off + 3] = v.toByte()
}

private fun getIntBE(b: ByteArray, off: Int): Int =
    ((b[off].toInt() and 0xFF) shl 24) or
        ((b[off + 1].toInt() and 0xFF) shl 16) or
        ((b[off + 2].toInt() and 0xFF) shl 8) or
        (b[off + 3].toInt() and 0xFF)

private fun crc32(b: ByteArray, off: Int, len: Int): Int {
    val c = CRC32()
    c.update(b, off, len)
    return c.value.toInt()
}