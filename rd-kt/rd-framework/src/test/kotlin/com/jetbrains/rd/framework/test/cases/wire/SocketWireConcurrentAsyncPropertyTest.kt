package com.jetbrains.rd.framework.test.cases.wire

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.bindTopLevel
import com.jetbrains.rd.framework.impl.AsyncRdProperty
import com.jetbrains.rd.framework.test.util.TestBase
import com.jetbrains.rd.framework.test.util.TestScheduler
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.spinUntil
import com.jetbrains.rd.util.threading.SynchronousScheduler
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicIntegerArray
import java.util.zip.CRC32

/**
 * Regression test for RIDER-127935.
 *
 * Several [AsyncRdProperty] instances are mutated concurrently from several different threads, each with a
 * large (multi-chunk) byte payload. `AsyncRdProperty.set` sends on the calling thread, so this turns the
 * wire into a multi-producer stream — the same shape that desynced the Rider frontend/backend protocol
 * when `RegistrySynchronizer` pushed the registry from `Dispatchers.Default`.
 *
 * Each payload carries an embedded {propIndex, iteration, crc32} header so the receiver can verify exact
 * integrity. Before the fix, concurrent producers corrupted `ByteBufferAsyncProcessor`'s chunk ring
 * (the consumer skipped filled chunks), which surfaced here as a crc/size mismatch, a deserialization
 * error (accumulated by [TestBase] and thrown at teardown), or a stalled delivery (timeout).
 */
class SocketWireConcurrentAsyncPropertyTest : TestBase() {

    companion object {
        private fun server(lifetime: Lifetime): Protocol = Protocol(
            "Server", Serializers(), SequentialIdentities(IdKind.Server), TestScheduler,
            SocketWire.Server(lifetime, TestScheduler, null, "TestServer"), lifetime
        )

        private fun client(lifetime: Lifetime, serverProtocol: Protocol): Protocol = Protocol(
            "Client", Serializers(), SequentialIdentities(IdKind.Client), TestScheduler,
            SocketWire.Client(lifetime, TestScheduler, (serverProtocol.wire as SocketWire.Server).port, "TestClient"),
            lifetime
        )

        private const val HEADER = 12 // [propIndex:int][iteration:int][crc32:int]

        private fun putInt(b: ByteArray, off: Int, v: Int) {
            b[off] = (v ushr 24).toByte()
            b[off + 1] = (v ushr 16).toByte()
            b[off + 2] = (v ushr 8).toByte()
            b[off + 3] = v.toByte()
        }

        private fun getInt(b: ByteArray, off: Int): Int =
            ((b[off].toInt() and 0xFF) shl 24) or
                ((b[off + 1].toInt() and 0xFF) shl 16) or
                ((b[off + 2].toInt() and 0xFF) shl 8) or
                (b[off + 3].toInt() and 0xFF)

        private fun crc(b: ByteArray, off: Int, len: Int): Int {
            val c = CRC32()
            c.update(b, off, len)
            return c.value.toInt()
        }

        private fun makePayload(propIndex: Int, iteration: Int, size: Int): ByteArray {
            val b = ByteArray(size)
            val seed = propIndex * 1_000_003 + iteration
            for (k in HEADER until size) b[k] = ((seed + k) and 0xFF).toByte()
            putInt(b, 0, propIndex)
            putInt(b, 4, iteration)
            putInt(b, 8, crc(b, HEADER, size - HEADER))
            return b
        }

        /** @return null if intact, otherwise a description of the corruption. */
        private fun verify(b: ByteArray, expectedSize: Int): String? {
            if (b.size != expectedSize) return "size=${b.size} != expected=$expectedSize"
            val propIndex = getInt(b, 0)
            val iteration = getInt(b, 4)
            val storedCrc = getInt(b, 8)
            val actualCrc = crc(b, HEADER, b.size - HEADER)
            if (storedCrc != actualCrc)
                return "crc mismatch prop=$propIndex it=$iteration stored=$storedCrc actual=$actualCrc"
            return null
        }
    }

    private lateinit var socketLifetime: Lifetime

    @BeforeEach
    fun setUp() {
        socketLifetime = lifetime.createNested().lifetime
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    fun concurrentLargeAsyncPropertySends() {
        val n = 16
        val rounds = 15
        val iterations = 20
        val payloadSize = 128_000 // > ByteBufferAsyncProcessor.DefaultChunkSize (16370) -> multi-chunk

        val serverProtocol = server(socketLifetime)
        val clientProtocol = client(socketLifetime, serverProtocol)

        val corruptions = ConcurrentLinkedQueue<String>()
        val lastReceivedIteration = AtomicIntegerArray(n)
        for (i in 0 until n) lastReceivedIteration.set(i, -1)

        val clientProps = ArrayList<AsyncRdProperty<ByteArray>>(n)

        for (i in 0 until n) {
            val id = RdId((100 + i).toLong())

            val serverProp = AsyncRdProperty(FrameworkMarshallers.ByteArray).also { it.master = false }
            serverProp.withId(id).apply { bindTopLevel(socketLifetime, serverProtocol, "asyncProp$i") }
            serverProp.change.adviseOn(socketLifetime, SynchronousScheduler) { value ->
                val err = verify(value, payloadSize)
                if (err != null) corruptions.add("prop#$i: $err")
                else lastReceivedIteration.set(i, getInt(value, 4))
            }

            val clientProp = AsyncRdProperty(FrameworkMarshallers.ByteArray).also { it.master = true }
            clientProp.withId(id).apply { bindTopLevel(socketLifetime, clientProtocol, "asyncProp$i") }
            clientProps.add(clientProp)
        }

        // Make sure the wires are connected so the sets actually race on the live socket.
        assertTrue(spinUntil(10_000) {
            (clientProtocol.wire as SocketWire.Base).connected.value &&
                (serverProtocol.wire as SocketWire.Base).connected.value
        }, "wires did not connect")

        // Each round is an independent concurrent burst; the desync is a timing race (~50% per burst),
        // so several rounds (with fail-fast) make reproduction reliable while the fixed wire stays clean.
        // Delivery is drained per round to keep the backlog (and runtime) bounded.
        var sentSoFar = 0
        var allDelivered = true
        for (round in 0 until rounds) {
            if (corruptions.isNotEmpty()) break
            val base = sentSoFar
            val startBarrier = CyclicBarrier(n)
            val senders = (0 until n).map { i ->
                Thread({
                    startBarrier.await()
                    for (it in 0 until iterations) {
                        clientProps[i].set(makePayload(i, base + it, payloadSize))
                    }
                }, "async-sender-$round-$i").apply { isDaemon = true; start() }
            }
            senders.forEach { it.join(60_000) }
            sentSoFar += iterations

            // Wait for this round to deliver its final value intact. A desync stalls delivery -> timeout.
            val target = sentSoFar - 1
            allDelivered = spinUntil(30_000) {
                corruptions.isNotEmpty() || (0 until n).all { lastReceivedIteration.get(it) == target }
            }
            if (!allDelivered) break
        }

        assertTrue(corruptions.isEmpty(), "byte-stream corruption detected: ${corruptions.toList()}")
        assertTrue(allDelivered, "timed out waiting for delivery; last received per prop = " +
            (0 until n).map { lastReceivedIteration.get(it) } + " (expected ${sentSoFar - 1})")
    }
}
