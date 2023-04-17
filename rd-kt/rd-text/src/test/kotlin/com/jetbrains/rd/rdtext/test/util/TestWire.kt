package com.jetbrains.rd.rdtext.test.util

import com.jetbrains.rd.framework.AbstractBuffer
import com.jetbrains.rd.framework.RdId
import com.jetbrains.rd.framework.base.WireBase
import com.jetbrains.rd.framework.createAbstractBuffer
import com.jetbrains.rd.util.Queue
import com.jetbrains.rd.util.reactive.IScheduler
import java.util.concurrent.ConcurrentLinkedQueue


class TestWire(scheduler : IScheduler) : WireBase() {
    lateinit var counterpart : TestWire

    val msgQ = ConcurrentLinkedQueue<RdMessage>()

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
        if (autoFlush) processAllMessages()
    }


    var autoFlush : Boolean = true
        get() = field
        set(value) {
            field = value
            if (value) processAllMessages()
        }

    fun processAllMessages() {
        while (!msgQ.isEmpty()) processOneMessage()
    }

    fun processOneMessage() {
        val msg = msgQ.poll() ?: return
        counterpart.messageBroker.dispatch(msg.id, msg.istream)
    }
}


class RdMessage (val id : RdId, val istream : AbstractBuffer)
