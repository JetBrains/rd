package com.jetbrains.rd.util.threading

import com.jetbrains.rd.util.*
import com.jetbrains.rd.util.string.condstr
import com.jetbrains.rd.util.time.InfiniteDuration
import java.time.Duration

//data class ByteArraySlice(val data: ByteArray, val offset: Int, val len: Int)
//fun OutputStream.write(slice: ByteArraySlice) = this.write(slice.data, slice.offset, slice.len)


class ByteBufferAsyncProcessor(val id : String,
                               val chunkSize: Int = DefaultChunkSize,
                               val processor: (Chunk) -> Unit) {

    enum class StateKind {
        Initialized,
        AsyncProcessing,
        Stopping,
        Terminating,
        Terminated;
    }

    class Chunk (chunkSize: Int) {
        companion object {
//            fun createCycledPair(chunkSize: Int): Chunk {
//                val chunk1 = Chunk(chunkSize)
//                val chunk2 = Chunk(chunkSize).apply { next = chunk1 }
//                chunk1.next = chunk2
//                return chunk1
//            }

            val empty = Chunk(0)
        }

        val data = ByteArray(chunkSize)
        var ptr = 0 //number of filled bytes in this chunk
        lateinit var next: Chunk
        var seqn = Long.MAX_VALUE

        fun checkEmpty(buffer: ByteBufferAsyncProcessor) : Boolean{
            if (ptr == 0) {
                assert(seqn == Long.MAX_VALUE) {"seqn == long.MaxValue, but: $seqn"}
                return true
            }

            if (buffer.acknowledgedSeqn < seqn)
                return false

            Reset()

            return true
        }

        val isNotProcessed : Boolean get() = seqn == Long.MAX_VALUE

        internal fun Reset() {
            seqn = Long.MAX_VALUE
            ptr = 0
        }
    }

    companion object {
        private const val DefaultChunkSize = 16370
        private const val DefaultShrinkIntervalMs = 30000
    }

    private val log = getLogger(this::class)
    private val lock = Object()
    private var asyncProcessingThread: Thread? = null

    private lateinit var chunkToFill : Chunk
    private lateinit var chunkToProcess : Chunk

    private val pauseReasons = HashSet<String>()

    private var processing = false

    var allDataProcessed : Boolean = true
        private set

    @Volatile
    var acknowledgedSeqn : Long = 0
        private set


    private var lastShrinkOrGrowTimeMs = System.currentTimeMillis()
    var shrinkIntervalMs = DefaultShrinkIntervalMs


    var state : StateKind = StateKind.Initialized
        private set


    init {
        reset()
    }

    val chunkCount : Int get() {
        synchronized(lock) {
            var chunk = chunkToFill.next
            var res = 1
            while (chunk != chunkToFill) {
                res++
                chunk = chunk.next
            }
            return res
        }
    }
    
    private fun cleanup0() {
        synchronized(lock) {
            state = StateKind.Terminated
            chunkToFill = Chunk.empty
            chunkToProcess = Chunk.empty
            allDataProcessed = true
        }
    }

    private fun terminate0(timeout: Duration, stateToSet: StateKind, action: String) : Boolean {
        synchronized(lock) {
            if (state == StateKind.Initialized) {
                log.debug {"Can't $action '$id', because it hasn't been started yet"}
                cleanup0()
                return true
            }

            if (state >= stateToSet) {
                log.debug {"Trying to $action async processor '$id' but it's in state '$state'" }
                return true
            }

            state = stateToSet
            lock.notifyAll()
        }

        val t = asyncProcessingThread?: return true

        t.join(timeout.toMillis())
        val threadStopped = !t.isAlive

        if (!threadStopped) catch { @Suppress("DEPRECATION") t.stop()}
        cleanup0()

        return threadStopped
    }

    fun acknowledge(seqn: Long) {
        synchronized(lock) {
            log.trace { "New acknowledged seqn received: $seqn" }
            if (seqn > acknowledgedSeqn) {
                acknowledgedSeqn = seqn
            } else {
//                throw IllegalStateException("Acknowledge($seqn) called, while next seqn MUST BE greater than `$acknowledgedSeqn`")
                //it's ok ack came 2 times for same package, because if connection lost/resume client resend package with lower number and could receive packages with lower numbers
            }
        }
    }

    fun reprocessUnacknowledged() {
        require(Thread.currentThread() != asyncProcessingThread) {"Thread.currentThread() != asyncProcessingThread"}

        synchronized(lock) {
            while (processing)
                lock.wait(1)

            var chunk = chunkToFill.next
            while (chunk != chunkToFill) {
                if (!chunk.checkEmpty(this)) {
                    chunkToProcess = chunk
                    allDataProcessed = false
                    lock.notifyAll()
                    return
                } else {
                    chunk = chunk.next
                }
            }
        }
    }


    private fun threadProc() {
        while (true) {
            synchronized(lock) {
                if (state >= StateKind.Terminated) return

                while (allDataProcessed || pauseReasons.isNotEmpty()) {
                    if (state >= StateKind.Stopping) return
                    lock.wait()
                    if (state >= StateKind.Terminating) return
                }

                while (chunkToProcess.checkEmpty(this))
                    chunkToProcess = chunkToProcess.next


                if (chunkToFill == chunkToProcess) {
                    growConditionally()
                    chunkToFill = chunkToProcess.next
                }

                shrinkConditionally(chunkToProcess)

                assert(chunkToProcess.ptr > 0) { "chunkToProcess.ptr > 0" }
                assert(chunkToFill != chunkToProcess && chunkToFill.isNotProcessed) {
                    "chunkToFill != chunkToProcess && chunkToFill.isNotProcessed"
                }

                processing = true
            }

            try {
                processor(chunkToProcess)
            } catch(e: Exception) {
                log.error("Exception while processing byte queue", e)
            } finally {
                synchronized(lock) {
                    processing = false
                    chunkToProcess = chunkToProcess.next
                    if (chunkToProcess.ptr == 0)
                        allDataProcessed = true
                }
            }
        }
    }


    fun start() {
        synchronized(lock) {
            if (state != StateKind.Initialized) {
                log.debug { "Trying to START async processor '$id' but it's in state '$state'" }
                return
            }

            state = StateKind.AsyncProcessing

            asyncProcessingThread = Thread({threadProc()}, id).apply { isDaemon = true }
            asyncProcessingThread?.start()
        }
    }

    private fun reset() {
        chunkToFill = Chunk(chunkSize)
        chunkToFill.next = chunkToFill
        lastShrinkOrGrowTimeMs = System.currentTimeMillis()
        chunkToProcess = chunkToFill
    }

    //must be executed under `synchronized(lock)`
    private fun waitProcessingFinished() {
        if (Thread.currentThread() == asyncProcessingThread) //don't want to deadlock
            return

        while (processing)
            lock.wait(1)
    }

    fun clear() {
        require(Thread.currentThread() != asyncProcessingThread) {"Thread.currentThread() != asyncProcessingThread"}

        synchronized(lock) {
            log.debug { "Cleaning '$id', state=$state" }
            if (state >= StateKind.Stopping)
                return

            waitProcessingFinished()

            reset()
            allDataProcessed = true
        }
    }


    fun pause(reason: String) {
        synchronized(lock) {
            if (state >= StateKind.Stopping)
                return

            val alreadyHadReason = !pauseReasons.add(reason)
            log.debug { "PAUSE ('$reason') ${alreadyHadReason.condstr { "<already has this pause reason> " }}:: {id = $id, state = '$state'}" }
            waitProcessingFinished()
        }
    }

    fun resume(reason: String) {
        synchronized(lock) {
            pauseReasons.remove(reason)
            val unpaused = pauseReasons.size == 0
            log.debug { (if (unpaused) "RESUME" else "Remove pause reason('$reason')") + " :: {id = $id, state = '$state'}" }
            lock.notifyAll()
        }
    }

    fun stop(timeout: Duration = InfiniteDuration) = terminate0(timeout, StateKind.Stopping, "STOP")
    fun terminate(timeout: Duration = InfiniteDuration) = terminate0(timeout, StateKind.Terminating, "TERMINATE")

    fun put(newData: ByteArray, offset: Int = 0, count: Int = newData.size) {
        synchronized(lock) {
            if (state >= StateKind.Stopping) return

            var ptr = 0
            while (ptr < count) {
                assert(chunkToFill.isNotProcessed) {"chunkToFill.isNotProcessed"}

                val rest = count - ptr
                val available = chunkSize - chunkToFill.ptr

                if (available > 0) {
                    val copylen = Math.min(rest, available)
                    System.arraycopy(newData, ptr + offset, chunkToFill.data, chunkToFill.ptr, copylen)
                    chunkToFill.ptr += copylen
                    ptr += copylen

                } else {
                    growConditionally()
                    chunkToFill = chunkToFill.next
                }
            }

            if (allDataProcessed) { //speedup
                allDataProcessed = false
                lock.notify()
            }
        }
    }

    private fun growConditionally() {
        if (chunkToFill.next.checkEmpty(this))
            return

        log.trace {"Grow: $chunkSize bytes" }
        chunkToFill.next = Chunk(chunkSize).apply { next = chunkToFill.next }
        lastShrinkOrGrowTimeMs = System.currentTimeMillis()
    }

    private fun shrinkConditionally(upTo: Chunk) {
        assert(chunkToFill != upTo) {"chunkToFill != upTo"}

        val now = System.currentTimeMillis()
        if (now - lastShrinkOrGrowTimeMs <= shrinkIntervalMs)
            return

        lastShrinkOrGrowTimeMs = now

        while (true) {
            val toRemove = chunkToFill.next
            if (toRemove == upTo || !toRemove.checkEmpty(this))
                break

            log.trace {"Shrink: $chunkSize bytes, seqN: $toRemove" }
            chunkToFill.next = toRemove.next
        }
    }
}


