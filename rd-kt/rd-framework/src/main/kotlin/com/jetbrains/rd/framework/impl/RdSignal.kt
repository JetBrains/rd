package com.jetbrains.rd.framework.impl

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.util.Logger
import com.jetbrains.rd.util.error
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

    override fun onWireReceived(proto: IProtocol, buffer: AbstractBuffer, ctx: SerializationCtx, dispatchHelper: IRdWireableDispatchHelper) {
        val value = valueSerializer.read(ctx, buffer)
        logReceived.trace {"onWireReceived:: signal `$location` ($rdid):: value = ${value.printToString()}"}
        dispatchHelper.dispatch(scheduler) {
            logReceived.trace {"dispatched:: signal `$location` ($rdid):: value = ${value.printToString()}"}
            signal.fire(value)
        }
    }

    //protocol init, don't mess with initializer above
    override fun preInit(lifetime: Lifetime, proto: IProtocol) {
        super.preInit(lifetime, proto)
        proto.wire.advise(lifetime, this)
    }

    override var scheduler: IScheduler? = null

    override fun fire(value: T) {
        assertThreading()

        val proto = protocol
        val ctx = serializationContext

        if (proto == null || ctx == null)
            return

        val wire = proto.wire

        wire.send(rdid) { buffer ->
            logSend.trace { "signal `$location` ($rdid):: value = ${value.printToString()}" }
            valueSerializer.write(ctx, buffer, value)
        }
        signal.fire(value)
    }


    override fun advise(lifetime: Lifetime, handler: (T) -> Unit) {
        if (isBound) assertThreading()
        signal.advise(lifetime, handler)
    }


    // todo remove this counterintuitive method
    @Deprecated("You should explicitly override the scheduler")
    override fun adviseOn(lifetime: Lifetime, scheduler: IScheduler, handler: (T) -> Unit) {
        if (isBound) assertThreading() //even if listener on pool thread, advise must be on main thread
        if (this.scheduler != null && this.scheduler !== scheduler) {
            Logger.Companion.root.error { "scheduler is already set: ${this.scheduler}, new scheduler: ${scheduler}" }
        }
        this.scheduler = scheduler
        signal.advise(lifetime, handler)
    }
}

