package com.jetbrains.rider.framework.base

import com.jetbrains.rider.framework.ISerializers
import com.jetbrains.rider.framework.Protocol
import com.jetbrains.rider.util.error
import com.jetbrains.rider.util.getLogger
import com.jetbrains.rider.util.reactive.IScheduler

abstract class RdReactiveBase : RdBindableBase(), IRdReactive {
    companion object {
        val logReceived = Protocol.sublogger("RECV")
        val logSend = Protocol.sublogger("SEND")

        val logAssert = getLogger<RdReactiveBase>()
    }

    //assertion
    override var async = false
    protected fun assertThreading() {
        if (!async && !defaultScheduler.isActive)
            logAssert.error("Must be executed on UI thread", IllegalStateException("|E| Wrong thread"))
    }
    protected fun assertBound() {
        if (!isBound) { throw IllegalStateException("Not bound") }
    }

    //delegated
    protected val serializers : ISerializers get() = protocol.serializers
    protected val defaultScheduler : IScheduler get() = protocol.scheduler

    //local change
    protected var isLocalChange = false
    internal fun <T> localChange(action: () -> T) : T {
        if (isBound && !async) assertThreading()

        assert(!isLocalChange){ "!isLocalChange" }

        isLocalChange = true
        try {
            return action()
        } finally {
            isLocalChange = false
        }
    }
}
