package com.jetbrains.rider.framework.test.util

import com.jetbrains.rider.framework.IProtocol
import com.jetbrains.rider.framework.RdId
import com.jetbrains.rider.framework.base.WireBase
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

class TestWire(protocol : IProtocol) : WireBase(protocol.scheduler) {
    lateinit var counterpart : TestWire

    val msgQ = java.util.concurrent.LinkedBlockingQueue<RdMessage>()

    var bytesWritten: Long = 0

    init {
        connected *= true
    }


    override fun send(id: RdId, writer: (OutputStream) -> Unit) {
        val ostream = ByteArrayOutputStream()
        writer(ostream)
        val istream = ByteArrayInputStream(ostream.toByteArray())

        bytesWritten += ostream.size()

        msgQ.add(RdMessage(id, istream))
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


class RdMessage (val id : RdId, val istream : InputStream)