package com.jetbrains.rider.framework.base

import com.jetbrains.rider.framework.*
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.lifetime.assertIsAlive
import com.jetbrains.rider.util.lifetime.plusAssign
import com.jetbrains.rider.util.onFalse
import com.jetbrains.rider.util.reactive.IScheduler
import org.apache.commons.logging.LogFactory

abstract class RdReactiveBase : RdBindableBase(), IRdReactive {
    companion object {
        val logReceived = Protocol.sublogger("RECEIVED")
        val logSend = Protocol.sublogger("SEND")

        val logAssert = LogFactory.getLog(RdReactiveBase::class.java)!!
    }

    //identification
    override var id: RdId = RdId.Null
    override fun identify(ids: IIdentities) { withId(ids.next()) }

    //assertion
    override var async = false
    protected fun assertThreading() {
        if (!async && !defaultScheduler.isActive)
            logAssert.error("Must be executed on UI thread", IllegalStateException("|E| Wrong thread"))
    }
    protected fun assertBound() = isBound.onFalse { throw IllegalStateException("Not bound") }

    //delegated
    protected val serializers : ISerializers get() = protocol.serializers
    protected val defaultScheduler : IScheduler get() = protocol.scheduler

    //local change
    protected var isLocalChange = false
    protected fun <T> localChange(action: () -> T) : T {
        if (isBound && !async) assertThreading()

        assert(!isLocalChange){ "!isLocalChange" }

        isLocalChange = true
        try {
            return action()
        } finally {
            isLocalChange = false
        }
    }

    //binding
    override fun init(lifetime: Lifetime) {
        lifetime.assertIsAlive()
        defaultScheduler.assertThread()
        id.notNull()
        lifetime += {id = RdId.Null}
    }

}
