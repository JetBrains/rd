package com.jetbrains.rd.framework.base

import com.jetbrains.rd.framework.ISerializers
import com.jetbrains.rd.framework.Protocol
import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.reactive.IScheduler

abstract class RdReactiveBase : RdBindableBase(), IRdReactive {
    companion object {
        val logReceived = Protocol.sublogger("RECV")
        val logSend = Protocol.sublogger("SEND")

        val logAssert = getLogger<RdReactiveBase>()
    }

    private var masterOverriden : Boolean? = null
    var master : Boolean
        get() = masterOverriden ?: protocol.isMaster
        set(value) { masterOverriden = value }

    val wire get() = protocol.wire

    //assertion
    override var async = false
    protected fun assertThreading() {
        if (!async) {
            defaultScheduler.assertThread()
        }
    }
    protected fun assertBound() {
        if (!isBound) { throw IllegalStateException("Not bound: $location") }
    }

    //delegated
    protected val serializers : ISerializers get() = protocol.serializers
    protected val defaultScheduler : IScheduler get() = protocol.scheduler
    override val wireScheduler: IScheduler get() = defaultScheduler

    //local change
    var isLocalChange = false
        protected set(value) {
            field = value
        }

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
