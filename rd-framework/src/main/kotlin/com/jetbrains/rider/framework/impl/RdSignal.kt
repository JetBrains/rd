package com.jetbrains.rider.framework.impl

import com.jetbrains.rider.framework.ISerializer
import com.jetbrains.rider.framework.Polymorphic
import com.jetbrains.rider.framework.SerializationCtx
import com.jetbrains.rider.framework.AbstractBuffer
import com.jetbrains.rider.framework.base.RdReactiveBase
import com.jetbrains.rider.framework.base.withId
import com.jetbrains.rider.framework.readRdId
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.reactive.*
import com.jetbrains.rider.util.string.printToString
import com.jetbrains.rider.util.trace

class RdSignal<T>(val valueSerializer: ISerializer<T> = Polymorphic<T>()) : RdReactiveBase(), IAsyncSignal<T> {

    companion object {
        fun<T> read(ctx: SerializationCtx, buffer: AbstractBuffer, valueSerializer: ISerializer<T>): RdSignal<T> = RdSignal(valueSerializer).withId(buffer.readRdId())
        fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: RdSignal<*>) = value.rdid.write(buffer)
    }

    private object DEFAULT_SCHEDULER_MARKER : IScheduler {
        override fun flush() {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun queue(action: () -> Unit) = throw UnsupportedOperationException()

        override val isActive: Boolean
            get() = throw UnsupportedOperationException()
    }

    private val signal = Signal<T>()

    private val schedulerHolder = OptProperty<IScheduler>()
    private val lifetimeHolder = OptProperty<Lifetime>()
    override lateinit var serializationContext : SerializationCtx

    init {
        schedulerHolder.compose(lifetimeHolder) { sc, lf -> sc to lf }.advise(Lifetime.Eternal) { (sc, lf) ->
            wire.adviseOn(lf, rdid, if (sc == DEFAULT_SCHEDULER_MARKER) defaultScheduler else sc) { buffer ->
                val value = valueSerializer.read(serializationContext, buffer)
                logReceived.trace {"signal `${location()}` ($rdid):: value = ${value.printToString()}"}
                signal.fire(value)
            }
        }
    }

    //protocol init, don't mess with initializer above
    override fun init(lifetime: Lifetime) {
        super.init(lifetime)
        signal.name = name
        serializationContext = super.serializationContext
        lifetimeHolder.set(lifetime)
    }

    override fun fire(value: T) {
        assertBound()
        if (!async) assertThreading()
        //localChange {
        wire.send(rdid) { buffer ->
            logSend.trace {"signal `${location()}` ($rdid):: value = ${value.printToString()}"}
            valueSerializer.write(serializationContext, buffer, value)
        }
        signal.fire(value)
        //}
    }


    override fun advise(lifetime: Lifetime, handler: (T) -> Unit) {
        if (isBound) assertThreading()
        schedulerHolder.set(DEFAULT_SCHEDULER_MARKER)
        signal.advise(lifetime, handler)
    }


    override fun adviseOn(lifetime: Lifetime, scheduler: IScheduler, handler: (T) -> Unit) {
        if (isBound) assertThreading() //even if listener on pool thread, advise must be on main thread
        schedulerHolder.set(scheduler)
        signal.advise(lifetime, handler)
    }
}

