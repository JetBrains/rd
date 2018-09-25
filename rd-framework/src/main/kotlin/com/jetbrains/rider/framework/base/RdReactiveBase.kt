package com.jetbrains.rider.framework.base

import com.jetbrains.rider.framework.ISerializers
import com.jetbrains.rider.framework.Protocol
import com.jetbrains.rider.util.getLogger
import com.jetbrains.rider.util.reactive.IScheduler

abstract class RdReactiveBase : RdBindableBase(), IRdReactive {
    companion object {
        val logReceived = Protocol.sublogger("RECV")
        val logSend = Protocol.sublogger("SEND")

        val logAssert = getLogger<RdReactiveBase>()
    }

    val wire get() = protocol.wire

    //assertion
    override var async = false
    protected fun assertThreading() {
        if (!async) {
            defaultScheduler.assertThread()
        }
    }
    protected fun assertBound() {
        if (!isBound) { throw IllegalStateException("Not bound") }
    }

    //delegated
    protected val serializers : ISerializers get() = protocol.serializers
    protected val defaultScheduler : IScheduler get() = protocol.scheduler
    override val wireScheduler: IScheduler get() = defaultScheduler

    //local change
    protected var isLocalChange = false
    internal fun <T> localChange(action: () -> T) : T {
        if (isBound && !async) assertThreading()

        require(!isLocalChange){ "!isLocalChange" }

        isLocalChange = true
        try {
            return action()
        } finally {
            isLocalChange = false
        }
    }
}
