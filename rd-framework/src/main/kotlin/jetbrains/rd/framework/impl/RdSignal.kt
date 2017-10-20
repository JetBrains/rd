package com.jetbrains.rider.framework.impl

import com.jetbrains.rider.framework.*
import com.jetbrains.rider.framework.base.RdReactiveBase
import com.jetbrains.rider.framework.base.printToString
import com.jetbrains.rider.framework.base.withId
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.reactive.*
import com.jetbrains.rider.util.trace
import java.io.InputStream
import java.io.OutputStream

class RdSignal<T>(val valueSerializer: ISerializer<T> = Polymorphic<T>()) : RdReactiveBase(), ISignal<T>, IAsyncSignal<T> {

    companion object {
        fun<T> read(ctx: SerializationCtx, stream: InputStream, valueSerializer: ISerializer<T>): RdSignal<T> = RdSignal(valueSerializer).withId(stream.readRdId())
        fun write(ctx: SerializationCtx, stream: OutputStream, value: RdSignal<*>) = value.id.write(stream)
    }



    private val signal = Signal<T>()

    //null means default scheduler
    private val schedulerHolder = Trigger<IScheduler?>()
    private val lifetimeHolder = Trigger<Lifetime>()

    init {
        schedulerHolder.compose(Lifetime.Eternal, lifetimeHolder) { sc, lf ->
            wire.adviseOn(lf, id, sc?:defaultScheduler) { stream ->
                val value = valueSerializer.read(serializationContext, stream)
                logReceived.trace {"signal `${location()}` ($id):: value = ${value.printToString()}"}
                signal.fire(value)
            }
        }
    }

    //protocol init, don't mess with initializer above
    override fun init(lifetime: Lifetime) {
        super.init(lifetime)
        signal.name = name
        lifetimeHolder.value = lifetime
    }

    override fun fire(value: T) {
        assertBound()
        if (!async) assertThreading()
        //localChange {
        wire.send(id) { stream ->
            logSend.trace {"signal `${location()}` ($id):: value = ${value.printToString()}"}
            valueSerializer.write(serializationContext, stream, value)
        }
        signal.fire(value)
        //}
    }


    override fun advise(lifetime: Lifetime, handler: (T) -> Unit) {
        if (isBound) assertThreading()
        schedulerHolder.value = null
        signal.advise(lifetime, handler)
    }


    override fun adviseOn(lifetime: Lifetime, scheduler: IScheduler, handler: (T) -> Unit) {
        if (isBound) assertThreading() //even if listener on pool thread, advise must be on main thread
        schedulerHolder.value = scheduler
        signal.advise(lifetime, handler)
    }
}

