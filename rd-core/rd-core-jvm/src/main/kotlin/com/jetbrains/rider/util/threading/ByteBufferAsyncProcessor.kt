package com.jetbrains.rider.util.threading

import com.jetbrains.rider.util.*
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.time.InfiniteDuration
import java.io.OutputStream
import java.time.Duration

data class ByteArraySlice(val data: ByteArray, val offset: Int, val len: Int)
fun OutputStream.write(slice: ByteArraySlice) = this.write(slice.data, slice.offset, slice.len)



class ByteBufferAsyncProcessor(val id : String, val chunkSize: Int = ByteBufferAsyncProcessor.DefaultChunkSize, val processor: (ByteArraySlice) -> Unit) {

    enum class StateKind {
        Initialized,
        AsyncProcessing,
        Stopping,
        Terminating,
        Terminated;
    }

    private class Chunk (chunkSize: Int) {
        companion object {
            fun createCycledPair(chunkSize: Int) : Chunk {
                val chunk1 = Chunk(chunkSize)
                val chunk2 = Chunk(chunkSize).apply { next = chunk1 }
                chunk1.next = chunk2
                return chunk1
            }

            val empty = Chunk(0)
        }

        lateinit var next: Chunk
        var ptr = 0
        val data = ByteArray(chunkSize)
    }

    companion object {
        private const val DefaultChunkSize = 16380
        private const val DefaultShrinkIntervalMs = 30000
    }

    private val log = getLogger(this::class)
    private val lock = Object()
    private var lastShrinkOrGrowTimeMs = System.currentTimeMillis()
    private lateinit var asyncProcessingThread: Thread
    
    private var freeChunk : Chunk
    private var firstChunkToProcess : Chunk?

    private val pauseReasons = ArrayList<String>()

    var shrinkIntervalMs = DefaultShrinkIntervalMs


    var state : StateKind = StateKind.Initialized
        private set

    init {
        val chunk = Chunk.createCycledPair(chunkSize)
        freeChunk = chunk
        firstChunkToProcess = chunk
    }
    
    private fun cleanup0() {
        synchronized(lock) {
            state = StateKind.Terminated
            freeChunk = Chunk.empty
            firstChunkToProcess = null
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

        asyncProcessingThread.join(timeout.toMillis())

        val res = asyncProcessingThread.isAlive

        if (!res) catch { @Suppress("DEPRECATION") asyncProcessingThread.stop()}
        cleanup0()

        return res
    }


    private fun ThreadProc() {
        var chunk = firstChunkToProcess!!
        firstChunkToProcess = null

        while (true) {
            synchronized(lock) {
                if (state >= StateKind.Terminated) return

                while (chunk.ptr == 0 || pauseReasons.isNotEmpty()) {
                    if (state >= StateKind.Stopping) return
                    lock.wait()
                    if (state >= StateKind.Terminating) return
                }

                if (freeChunk == chunk)
                    freeChunk = chunk.next

                // shrinking
                val now = System.currentTimeMillis()
                if (now - lastShrinkOrGrowTimeMs > shrinkIntervalMs) {
                    lastShrinkOrGrowTimeMs = now

                    while (freeChunk.next != chunk && freeChunk.ptr == 0) {
                        log.debug {"Shrink: $chunkSize bytes" }
                        if (freeChunk.next.ptr != 0)
                            log.debug {"freeChunk.next.ptr != 0"}

                        freeChunk.next = freeChunk.next.next
                    }
                }
            }

            try {
                processor(ByteArraySlice(chunk.data, 0, chunk.ptr))
            } catch(e: Exception) {
                log.error("Exception while processing byte queue", e)
            } finally {
                synchronized(lock) {
                    chunk.ptr = 0
                    chunk = chunk.next
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

            asyncProcessingThread = Thread({ThreadProc()}, id).apply { isDaemon = true }
            asyncProcessingThread.start()
        }
    }


    fun pause(lifetime: Lifetime, reason: String) {

        lifetime.bracket(
            { synchronized(lock) {
                    pauseReasons.add(reason)
                }
            },
            { synchronized(lock) {
                    pauseReasons.remove(reason)
                    lock.notify()
                }
            }
        )
    }

    fun stop(timeout: Duration = InfiniteDuration) = terminate0(timeout, StateKind.Stopping, "STOP")
    fun terminate(timeout: Duration = InfiniteDuration) = terminate0(timeout, StateKind.Terminating, "TERMINATE")

    fun put(newData: ByteArray, offset: Int = 0, count: Int = newData.size) {
        synchronized(lock) {
            if (state >= StateKind.Stopping) return

            var ptr = 0

            //speedup var, only to suppress redundant notifies
            val shouldNotify = freeChunk.ptr == 0 //everything processed (or we occasionally at 0 of some free chunk that isn't equal to proccessing chunk)

            while (ptr < count) {
                val rest = count - ptr
                val available = chunkSize - freeChunk.ptr

                if (available > 0) {
                    val copylen = Math.min(rest, available)
                    System.arraycopy(newData, ptr + offset, freeChunk.data, freeChunk.ptr, copylen)
                    freeChunk.ptr += copylen
                    ptr += copylen

                } else {
                    if (freeChunk.next.ptr != 0) {
                        log.debug {"Grow: $chunkSize bytes" }
                        freeChunk.next = Chunk(chunkSize).apply { next = freeChunk.next }
                        lastShrinkOrGrowTimeMs = System.currentTimeMillis()
                    }
                    freeChunk = freeChunk.next
                }
            }

            if (shouldNotify)
                lock.notify()
        }
    }
}


