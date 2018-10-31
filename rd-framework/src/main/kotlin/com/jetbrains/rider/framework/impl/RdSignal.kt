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

    companion object : ISerializer<RdSignal<*>>{
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): RdSignal<*> = read(ctx, buffer, Polymorphic<Any?>())
        fun<T> read(ctx: SerializationCtx, buffer: AbstractBuffer, valueSerializer: ISerializer<T>): RdSignal<T> = RdSignal(valueSerializer).withId(buffer.readRdId())
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: RdSignal<*>) = value.rdid.write(buffer)
    }

    private val signal = Signal<T>()
    override val changing: Boolean get() = signal.changing

    override lateinit var wireScheduler: IScheduler
    override lateinit var serializationContext : SerializationCtx


    override fun onWireReceived(buffer: AbstractBuffer) {
        val value = valueSerializer.read(serializationContext, buffer)
        logReceived.trace {"signal `$location` ($rdid):: value = ${value.printToString()}"}
        signal.fire(value)
    }

    //protocol init, don't mess with initializer above
    override fun init(lifetime: Lifetime) {
        super.init(lifetime)
        serializationContext = super.serializationContext
        wireScheduler = defaultScheduler
        wire.advise(lifetime, this)

    }

    override fun fire(value: T) {
//        assertBound()
        if (!async) assertThreading()
        //localChange {
        wire.send(rdid) { buffer ->
            logSend.trace {"signal `$location` ($rdid):: value = ${value.printToString()}"}
            valueSerializer.write(serializationContext, buffer, value)
        }
        signal.fire(value)
        //}
    }


    override fun advise(lifetime: Lifetime, handler: (T) -> Unit) {
        if (isBound) assertThreading()
        signal.advise(lifetime, handler)
    }


    override fun adviseOn(lifetime: Lifetime, scheduler: IScheduler, handler: (T) -> Unit) {
        if (isBound) assertThreading() //even if listener on pool thread, advise must be on main thread
        this.wireScheduler = scheduler
        signal.advise(lifetime, handler)
    }
}

