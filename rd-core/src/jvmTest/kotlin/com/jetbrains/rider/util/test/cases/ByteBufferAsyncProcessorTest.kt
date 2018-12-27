package com.jetbrains.rider.util.test.cases

import com.jetbrains.rider.util.threading.ByteBufferAsyncProcessor
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.concurrent.thread

class ByteBufferAsyncProcessorTest {
    @Test
    fun testSimpleStartTerminate() {
        val processor = ByteBufferAsyncProcessor("") { error("Something was received but was not supposed to") }

        assert(processor.state == ByteBufferAsyncProcessor.StateKind.Initialized)
        processor.start()
        assert(processor.state == ByteBufferAsyncProcessor.StateKind.AsyncProcessing)
        processor.terminate()
        assert(processor.state == ByteBufferAsyncProcessor.StateKind.Terminated)
    }

    @Test
    fun testSimpleStartStop() {
        val processor = ByteBufferAsyncProcessor("") { error("Something was received but was not supposed to") }

        assert(processor.state == ByteBufferAsyncProcessor.StateKind.Initialized)
        processor.start()
        assert(processor.state == ByteBufferAsyncProcessor.StateKind.AsyncProcessing)
        processor.stop()
        assert(processor.state == ByteBufferAsyncProcessor.StateKind.Terminated)
    }

    @Test(timeout = 10_000)
    fun testSimpleDataTransfer() {
        var receivedCounter = 0
        val bias = 80
        val processor = ByteBufferAsyncProcessor("") {
            for (i in it.offset until it.offset+it.len) {
                assert(it.data[i] == ((i - it.offset + bias + receivedCounter) and 0xff).toByte())
            }
            receivedCounter += it.len
        }

        var send = true
        val sendThread = thread(name = "Send Thread") {
            while(send) {
                processor.put(ByteArray(256) { (it + bias).toByte() })
            }
        }
        processor.start()

        Thread.sleep(1000)

        send = false
        processor.stop()
        sendThread.join()

        assert(receivedCounter > 0) { "No bytes were received" }
    }

    @Test(timeout = 10_000)
    fun testMultiThreadedDataTransfer() {
        var receivedCounter = 0
        val bias = 80
        var hadBadSequence = false
        val processor = ByteBufferAsyncProcessor("") {
            for (i in 0 until it.len) {
                hadBadSequence = hadBadSequence or (it.data[i + it.offset] != ((i + bias + receivedCounter) and 0xff).toByte())
            }
            receivedCounter += it.len
        }

        val numSendThreads = 2

        var send = true
        val sendThreads = (1..numSendThreads).map {
            thread(name = "Send Thread $it") {
                while (send) {
                    processor.put(ByteArray(256) { (it + bias).toByte() })
                }
            }
        }
        processor.start()

        Thread.sleep(1000)

        send = false
        processor.stop()
        sendThreads.forEach { it.join() }

        assert(receivedCounter > 0) { "No bytes were received" }
        assert(!hadBadSequence) { "Byte sequence was wrong" }
    }

    // thread leak validation
    private var activeThreadsBeforeTest = 0

    @Before
    fun storeActiveThreads() {
        activeThreadsBeforeTest = Thread.activeCount()
    }

    @After
    fun verifyNoLeakedThreads() {
        assert(Thread.activeCount() == activeThreadsBeforeTest) { "Some threads leaked" }
    }
}