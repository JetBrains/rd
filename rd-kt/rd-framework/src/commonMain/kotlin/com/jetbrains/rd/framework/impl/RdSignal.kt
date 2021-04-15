package com.jetbrains.rd.framework.impl

import com.jetbrains.rd.framework.ISerializer
import com.jetbrains.rd.framework.Polymorphic
import com.jetbrains.rd.framework.SerializationCtx
import com.jetbrains.rd.framework.AbstractBuffer
import com.jetbrains.rd.framework.base.IRdBindable
import com.jetbrains.rd.framework.base.RdReactiveBase
import com.jetbrains.rd.framework.base.withId
import com.jetbrains.rd.framework.readRdId
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.*
import com.jetbrains.rd.util.string.printToString
import com.jetbrains.rd.util.trace

@Suppress("UNUSED_PARAMETER")
class RdSignal<T>(val valueSerializer: ISerializer<T> = Polymorphic<T>()) : RdReactiveBase(), IAsyncSignal<T> {



    companion object : ISerializer<RdSignal<*>>{
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): RdSignal<*> = read(ctx, buffer, Polymorphic<Any?>())
        fun<T> read(ctx: SerializationCtx, buffer: AbstractBuffer, valueSerializer: ISerializer<T>): RdSignal<T> = RdSignal(valueSerializer).withId(buffer.readRdId())
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: RdSignal<*>) = value.rdid.write(buffer)
    }

    override fun deepClone(): IRdBindable = RdSignal(valueSerializer)

    private val signal = Signal<T>()
    override val changing: Boolean get() = signal.changing

    override lateinit var wireScheduler: IScheduler
    override lateinit var serializationContext : SerializationCtx

    override var scheduler: IScheduler
        get() = wireScheduler
        set(value) { wireScheduler = value }

    override fun onWireReceived(buffer: AbstractBuffer) {
        val value = valueSerializer.read(serializationContext, buffer)
        logReceived.trace {"signal `$location` ($rdid):: value = ${value.printToString()}"}
        signal.fire(value)
    }

    //protocol init, don't mess with initializer above
    override fun init(lifetime: Lifetime) {
        super.init(lifetime)
        serializationContext = super.serializationContext
        if (!this::wireScheduler.isInitialized)
            wireScheduler = defaultScheduler
        wire.advise(lifetime, this)

    }

    override fun fire(value: T) {
//        assertBound()
        //localChange {
        if (isBound) {
            assertThreading()
            wire.send(rdid) { buffer ->
                logSend.trace { "signal `$location` ($rdid):: value = ${value.printToString()}" }
                valueSerializer.write(serializationContext, buffer, value)
            }
        }
        signal.fire(value)
        //}
    }


    override fun advise(lifetime: Lifetime, handler: (T) -> Unit) {
        if (isBound) assertThreading()
        signal.advise(lifetime, handler)
    }


    // todo remove this counterintuitive method
    @Deprecated("You should explicitly override the scheduler")
    override fun adviseOn(lifetime: Lifetime, scheduler: IScheduler, handler: (T) -> Unit) {
        if (isBound) assertThreading() //even if listener on pool thread, advise must be on main thread
        this.wireScheduler = scheduler
        signal.advise(lifetime, handler)
    }
}

