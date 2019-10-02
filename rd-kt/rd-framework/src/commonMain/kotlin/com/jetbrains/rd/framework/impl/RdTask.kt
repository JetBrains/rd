package com.jetbrains.rd.framework.impl

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.util.*
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.reactive.OptProperty
import com.jetbrains.rd.util.reactive.hasValue
import com.jetbrains.rd.util.reactive.valueOrThrow
import com.jetbrains.rd.util.string.condstr
import com.jetbrains.rd.util.string.printToString
import com.jetbrains.rd.util.threading.SynchronousScheduler

fun<TReq, TRes> IRdCall<TReq, TRes>.startAndAdviseSuccess(request: TReq, onSuccess: (TRes) -> Unit) {
    startAndAdviseSuccess(Lifetime.Eternal, request, onSuccess)
}

fun<TReq, TRes> IRdCall<TReq, TRes>.startAndAdviseSuccess(lifetime: Lifetime, request: TReq, onSuccess: (TRes) -> Unit) {
    start(request).result.advise(lifetime) {
        if (it is RdTaskResult.Success<TRes>) onSuccess(it.value)
    }
}


open class RdTask<T> : IRdTask<T> {
    override val result = OptProperty<RdTaskResult<T>>()
    fun set(v : T) = result.set(RdTaskResult.Success(v))

    companion object {
        fun<T> faulted(error: Throwable) = RdTask<T>().apply { this.result.set(RdTaskResult.Fault(error)) }
        fun<T> fromResult(value: T) = RdTask<T>().apply { this.set(value) }
    }
}


class WiredRdTask<T>(val lifetimeDef: LifetimeDefinition, val call: RdCall<*,*>, override val rdid: RdId, val scheduler: IScheduler) : RdTask<T>(), IRdWireable {

    override val wireScheduler: IScheduler get() = SynchronousScheduler

    init {
        call.wire.advise(lifetimeDef.lifetime, this)
        lifetimeDef.lifetime.onTerminationIfAlive { result.setIfEmpty(RdTaskResult.Cancelled()) }
    }

    override fun onWireReceived(buffer: AbstractBuffer) {
        //todo don't write anything if request is dropped

        val resultFromWire = RdTaskResult.read(call.serializationContext, buffer, call.responseSzr) as RdTaskResult<T>
        RdReactiveBase.logReceived.trace { "call `${call.location}` (${call.rdid}) received response for task'$rdid' : ${resultFromWire.printToString()} " }

        scheduler.queue {
            if (result.hasValue) {
                RdReactiveBase.logReceived.trace { "call `${call.location}` (${call.rdid}) response was dropped, task result is: ${result.valueOrNull}" }
//              if (isBound && defaultScheduler.isActive && requests.containsKey(taskId)) RdReactiveBase.logAssert.error { "MainThread: ${defaultScheduler.isActive}, taskId=$taskId " }
            } else {
                //todo could be race condition in sync mode in case of Timeout, but it's not really valid case
                //todo but now we could start task on any scheduler - need interlocks in property
                result.setIfEmpty(resultFromWire)
            }
            lifetimeDef.terminate()
        }

    }
}

class RpcTimeouts(val warnAwaitTime : Long, val errorAwaitTime : Long)
{
    companion object {
        val default = RpcTimeouts(200L, 3000L)
        val longRunning = RpcTimeouts(10000L, 15000L)
        val infinite = RpcTimeouts(60000L, Long.MAX_VALUE / 1000000) // Long.MAX_VALUE nanoseconds in milliseconds
    }
}






@Suppress("UNCHECKED_CAST")
//Can't be constructed by constructor, only by deserializing counterpart: RdEndpoint
class RdCall<TReq, TRes>(internal val requestSzr: ISerializer<TReq> = Polymorphic<TReq>(),
                         internal val responseSzr: ISerializer<TRes> = Polymorphic<TRes>()) : RdReactiveBase(), IRdCall<TReq, TRes> {

    companion object : ISerializer<RdCall<*,*>>{
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): RdCall<*, *> = read(ctx, buffer, Polymorphic<Any?>(), Polymorphic<Any?>())

        fun <TReq, TRes> read(ctx: SerializationCtx, buffer: AbstractBuffer, requestSzr: ISerializer<TReq>, responseSzr: ISerializer<TRes>): RdCall<TReq, TRes> {
            return RdCall(requestSzr, responseSzr).withId(buffer.readRdId())
        }
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: RdCall<*, *>) {
            buffer.writeRdId(value.rdid)
        }

        var respectSyncCallTimeouts = true
    }

    override lateinit var serializationContext : SerializationCtx
    override val wireScheduler: IScheduler get() = SynchronousScheduler

    private var handler: ((Lifetime, TReq) -> RdTask<TRes>)? = null

    lateinit var bindLifetime : Lifetime

    override fun init(lifetime: Lifetime) {
        super.init(lifetime)
        bindLifetime = lifetime

        //Because we advise on Synchronous Scheduler: RIDER-10986
        serializationContext = super.serializationContext
        wire.advise(lifetime, this)
    }

    override fun sync(request: TReq, timeouts: RpcTimeouts?) : TRes {

        val task = startInternal(request, true, SynchronousScheduler)

        val effectiveTimeouts = if (respectSyncCallTimeouts) timeouts ?: RpcTimeouts.default else RpcTimeouts.infinite

        val freezeTime = measureTimeMillis {
            if (!task.wait(effectiveTimeouts.errorAwaitTime) {
                    if (protocol.scheduler.isActive)
                        containingExt?.pumpScheduler()
                }
            )
                throw TimeoutException("Sync execution of rpc `$location` is timed out in ${effectiveTimeouts.errorAwaitTime} ms")
        }
        if (freezeTime > effectiveTimeouts.warnAwaitTime) logAssert.error {"Sync execution of rpc `$location` executed too long: $freezeTime ms "}
        return task.result.valueOrThrow.unwrap()

    }


    override fun start(request: TReq, responseScheduler: IScheduler?) : IRdTask<TRes> {
        return startInternal(request, false, responseScheduler ?: protocol.scheduler) as RdTask<TRes>
    }

    fun start(request: TReq) = start(request, null)


    private fun startInternal(request: TReq, sync: Boolean, scheduler: IScheduler) : IRdTask<TRes> {
        assertBound()
        if (!async) assertThreading()

        val taskId = protocol.identity.next(rdid)

        //todo bindLifetime -> arbitrary lifetime
        val task = WiredRdTask<TRes>(bindLifetime.createNested(), this, taskId, scheduler)

        wire.send(rdid) { buffer ->
            logSend.trace { "call `$location`::($rdid) send${sync.condstr {" SYNC"}} request '$taskId' : ${request.printToString()} " }
            taskId.write(buffer)
            requestSzr.write(serializationContext, buffer, request)
        }

        return task
    }

    /**
     * Assigns a handler that executes the API asynchronously.
     */
    fun set(handler: (Lifetime, TReq) -> RdTask<TRes>) {
        require(this.handler == null) {"handler is set already"}
        this.handler = handler
    }
    constructor(handler:(Lifetime, TReq) -> RdTask<TRes>) : this() { set(handler) }

    /**
     * Assigns a handler that executes the API synchronously.
     */
    fun set(handler: (TReq) -> TRes) = set { _, req -> RdTask.fromResult(handler(req)) }

    constructor(handler: (TReq) -> TRes) : this () {set(handler)}

    override fun onWireReceived(buffer: AbstractBuffer) {
        val taskId = RdId.read(buffer)
        val request : TReq = requestSzr.read(serializationContext, buffer)

        logReceived.trace {"endpoint `$location`::($rdid) taskId=($taskId) request = ${request.printToString()}" + (handler == null).condstr { " BUT handler is NULL" }}

        //little bit monadic programming here
        Result.wrap { handler!!(bindLifetime, request) }
                .transform( {it}, { RdTask.faulted(it) })
                .result.advise(bindLifetime) { result ->
            logSend.trace { "endpoint `$location`::($rdid) taskId=($taskId) response = ${result.printToString()}" }
            wire.send(taskId) { buffer ->
                RdTaskResult.write(serializationContext, buffer, result, responseSzr)
            }
        }
    }
}
