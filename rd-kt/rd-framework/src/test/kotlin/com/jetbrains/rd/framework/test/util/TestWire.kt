package com.jetbrains.rd.framework.test.util

import com.jetbrains.rd.framework.RdId
import com.jetbrains.rd.framework.AbstractBuffer
import com.jetbrains.rd.framework.base.WireBase
import com.jetbrains.rd.framework.createAbstractBuffer
import com.jetbrains.rd.util.Queue
import com.jetbrains.rd.util.reactive.IScheduler

class TestWire(scheduler : IScheduler) : WireBase(scheduler) {
    lateinit var counterpart : TestWire

    val msgQ = Queue<RdMessage>()

    var bytesWritten: Long = 0

    init {
        connected.set(true)
    }


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


class RdMessage (val id : RdId, val istream : AbstractBuffer)