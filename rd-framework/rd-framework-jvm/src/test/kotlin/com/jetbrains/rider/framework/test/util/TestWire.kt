package com.jetbrains.rider.framework.test.util

import com.jetbrains.rider.framework.RdId
import com.jetbrains.rider.framework.UnsafeBuffer
import com.jetbrains.rider.framework.AbstractBuffer
import com.jetbrains.rider.framework.base.WireBase
import com.jetbrains.rider.util.reactive.IScheduler

class TestWire(scheduler : IScheduler) : WireBase(scheduler) {
    lateinit var counterpart : TestWire

    val msgQ = java.util.concurrent.LinkedBlockingQueue<RdMessage>()

    var bytesWritten: Long = 0

    init {
        connected.set(true)
    }


    override fun send(id: RdId, writer: (AbstractBuffer) -> Unit) {
        require(!id.isNull)

        val ostream = UnsafeBuffer(10)
        writer(ostream)

        bytesWritten += ostream.position

        ostream.position = 0

        msgQ.add(RdMessage(id, ostream))
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


class RdMessage (val id : RdId, val istream : UnsafeBuffer)