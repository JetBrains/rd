package com.jetbrains.rd.framework.test.util

import com.jetbrains.rd.framework.AbstractBuffer
import com.jetbrains.rd.framework.RdId
import com.jetbrains.rd.framework.RdMessage
import com.jetbrains.rd.framework.base.WireBase
import com.jetbrains.rd.framework.createAbstractBuffer
import com.jetbrains.rd.util.reactive.IScheduler
import java.util.concurrent.ConcurrentLinkedQueue

class TestWire(val scheduler : IScheduler) : WireBase() {
    lateinit var counterpart : TestWire

    val msgQ = ConcurrentLinkedQueue<RdMessage>()

    var bytesWritten: Long = 0

    init {
        connected.set(true)
    }

    val hasMessages
        get() =  msgQ.size > 0

    override fun send(id: RdId, writer: (AbstractBuffer) -> Unit) {
        require(!id.isNull)

        val ostream = createAbstractBuffer()
        contexts.writeCurrentMessageContext(ostream)
        writer(ostream)

        bytesWritten += ostream.position

        ostream.position = 0

        msgQ.offer(RdMessage(id, ostream))
        if (autoFlush) {
            processAllMessages()
        }
    }


    var autoFlush : Boolean = true
        get() = field
        set(value) {
            field = value
            if (value) {
                processAllMessages()
            }
        }

    fun processAllMessages() {
        scheduler.invokeOrQueue { // emulate normal wire behavior in that all messages are processed in a single thread
            while (!msgQ.isEmpty()) processOneMessage()
        }
    }

    fun processOneMessage() {
        val msg = msgQ.poll() ?: return
        counterpart.messageBroker.dispatch(msg.id, msg.istream)
    }
}