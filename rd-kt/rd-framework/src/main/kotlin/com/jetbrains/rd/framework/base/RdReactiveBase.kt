package com.jetbrains.rd.framework.base

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.isNotAlive
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.trace

abstract class RdReactiveBase : RdBindableBase(), IRdReactive {
    companion object {
        val logReceived = Protocol.sublogger("RECV")
        val logSend = Protocol.sublogger("SEND")

        val logAssert = getLogger<RdReactiveBase>()
    }

    private var masterOverriden : Boolean? = null
    var master : Boolean
        get() = masterOverriden ?: protocol?.isMaster ?: false
        set(value) { masterOverriden = value }

    //assertion
    override var async = false
    protected open fun assertThreading() {
        if (!async && AllowBindingCookie.isBindNotAllowed) {
            val proto = protocol ?: return
            proto.scheduler.assertThread(this)
        }
    }
    protected fun assertBound() {
        if (!isBound) { throw IllegalStateException("Not bound: $location") }
    }


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

    final override fun onWireReceived(buffer: AbstractBuffer, dispatchHelper: IRdWireableDispatchHelper) {
        val proto = protocol
        val ctx = serializationContext
        if (proto == null || ctx == null || dispatchHelper.lifetime.isNotAlive) {
            logReceived.trace { "$this is not bound. Message for (${dispatchHelper.rdId} will not be processed" }
            return
        }

        return onWireReceived(proto, buffer, ctx, dispatchHelper)
    }

    abstract fun onWireReceived(proto: IProtocol, buffer: AbstractBuffer, ctx: SerializationCtx, dispatchHelper: IRdWireableDispatchHelper)
}
