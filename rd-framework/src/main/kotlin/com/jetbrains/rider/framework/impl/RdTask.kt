package com.jetbrains.rider.framework.impl

import com.jetbrains.rider.framework.*
import com.jetbrains.rider.framework.base.RdReactiveBase
import com.jetbrains.rider.framework.base.withId
import com.jetbrains.rider.util.*
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.reactive.IScheduler
import com.jetbrains.rider.util.reactive.OptProperty
import com.jetbrains.rider.util.reactive.hasValue
import com.jetbrains.rider.util.reactive.valueOrThrow
import com.jetbrains.rider.util.string.condstr
import com.jetbrains.rider.util.string.printToString
import com.jetbrains.rider.util.threading.SynchronousScheduler
import kotlin.jvm.Volatile

fun<TReq, TRes> IRdCall<TReq, TRes>.startAndAdviseSuccess(request: TReq, onSuccess: (TRes) -> Unit) {
    startAndAdviseSuccess(Lifetime.Eternal, request, onSuccess)
}

fun<TReq, TRes> IRdCall<TReq, TRes>.startAndAdviseSuccess(lifetime: Lifetime, request: TReq, onSuccess: (TRes) -> Unit) {
    start(request).result.advise(lifetime) {
        if (it is RdTaskResult.Success<TRes>) onSuccess(it.value)
    }
}


class RdTask<T> : IRdTask<T> {
    companion object {
        fun<T> faulted(error: Throwable) = RdTask<T>().apply { this.result.set(RdTaskResult.Fault(error)) }
        fun<T> fromResult(value: T) = RdTask<T>().apply { this.set(value) }
    }

    override val result = OptProperty<RdTaskResult<T>>()
    fun set(v : T) = result.set(RdTaskResult.Success(v))
}

class RpcTimeouts(val warnAwaitTime : Long, val errorAwaitTime : Long)
{
    companion object {
        val default = RpcTimeouts(200L, 3000L)
        val longRunning = RpcTimeouts(10000L, 15000L)
        val maximal = RpcTimeouts(30000L, 30000L)
    }
}

@Suppress("UNCHECKED_CAST")
//Can't be constructed by constructor, only by deserializing counterpart: RdEndpoint
class RdCall<TReq, TRes>(private val requestSzr: ISerializer<TReq> = Polymorphic<TReq>(),
                         private val responseSzr: ISerializer<TRes> = Polymorphic<TRes>()) : RdReactiveBase(), IRdCall<TReq, TRes> {

    companion object : ISerializer<RdCall<*,*>>{
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): RdCall<*, *> = read(ctx, buffer, Polymorphic<Any?>(), Polymorphic<Any?>())

        fun <TReq, TRes> read(ctx: SerializationCtx, buffer: AbstractBuffer, requestSzr: ISerializer<TReq>, responseSzr: ISerializer<TRes>): RdCall<TReq, TRes> {
            return RdCall(requestSzr, responseSzr).withId(buffer.readRdId())
        }
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: RdCall<*, *>) {
            buffer.writeRdId(value.rdid)
//            throw IllegalStateException("Serialization of RdCall (${value.id}) is not allowed. The only valid option is to deserialize RdEndpoint.")
        }

        var respectSyncCallTimeouts = true
    }

    private val requests = concurrentMapOf<RdId, Pair<IScheduler, RdTask<TRes>>>()

    @Volatile
    private var syncTaskId: RdId? = null
    override lateinit var serializationContext : SerializationCtx
    override val wireScheduler: IScheduler get() = SynchronousScheduler

    override fun init(lifetime: Lifetime) {
        super.init(lifetime)

        //Because we advise on Synchronous Scheduler: RIDER-10986
        serializationContext = super.serializationContext
        wire.advise(lifetime, this)
    }

    override fun onWireReceived(buffer: AbstractBuffer) {
        val taskId = buffer.readRdId()
        val request = requests[taskId]

        if (request == null) {
            logReceived.trace { "call `$location` ($rdid) received response '$taskId' but it was dropped" }

        } else {
            val result = RdTaskResult.read(serializationContext, buffer, responseSzr)
            logReceived.trace { "call `$location` ($rdid) received response '$taskId' : ${result.printToString()} " }

            val (scheduler, task) = request
            scheduler.queue {
                if (task.result.hasValue) {
                    logReceived.trace { "call `$location` ($rdid) response was dropped, task result is: ${task.result.valueOrNull}" }
                    if (isBound && defaultScheduler.isActive && requests.containsKey(taskId)) logAssert.error { "MainThread: ${defaultScheduler.isActive}, taskId=$taskId " }
                } else {
                    //todo could be race condition in sync mode in case of Timeout, but it's not really valid case
                    //todo but now we could start task on any scheduler - need interlocks in property
                    task.result.set(result)
                    requests.remove(taskId)
                }
            }
        }
    }

    override fun sync(request: TReq, timeouts: RpcTimeouts?) : TRes {
        try {
            val task = startInternal(request, true, SynchronousScheduler)

            val effectiveTimeouts = if (respectSyncCallTimeouts) timeouts ?: RpcTimeouts.default else RpcTimeouts.maximal

            val freezeTime = measureTimeMillis {
                if (!task.wait(effectiveTimeouts.errorAwaitTime) {
                        if (protocol.scheduler.isActive)
                            containingExt?.pumpScheduler()
                    }
                )
                    throw TimeoutException("Sync execution of rpc `$location` is timed out in ${effectiveTimeouts.errorAwaitTime} ms")
            }
            if (freezeTime > effectiveTimeouts.warnAwaitTime) logAssert.error {"Sync execution of rpc `$location` executed too long: $freezeTime ms "}
            return (task.result.valueOrThrow as RdTaskResult<TRes>).unwrap()
        } finally {
            syncTaskId = null
        }
    }




    override fun start(request: TReq, responseScheduler: IScheduler?) : IRdTask<TRes> {
        return startInternal(request, false, responseScheduler ?: protocol.scheduler) as RdTask<TRes>
    }

    fun start(request: TReq) = start(request, null)


    private fun startInternal(request: TReq, sync: Boolean, scheduler: IScheduler) : IRdTask<*> {
        assertBound()
        if (!async) assertThreading()

        val taskId = protocol.identity.next(rdid)
        val task = RdTask<TRes>()
        requests.putUnique(taskId, scheduler to task)
        if (sync) {
            if (syncTaskId != null) throw IllegalStateException("Already exists sync task for call `$location`, taskId = $syncTaskId")
            syncTaskId = taskId
        }

        wire.send(rdid) { buffer ->
            logSend.trace { "call `$location`::($rdid) send${sync.condstr {" SYNC"}} request '$taskId' : ${request.printToString()} " }
            taskId.write(buffer)
            requestSzr.write(serializationContext, buffer, request)
        }

        return task
    }
}

/**
 * An API that is exposed to the remote process and can be invoked over the protocol.
 */
class RdEndpoint<TReq, TRes>(private val requestSzr: ISerializer<TReq> = Polymorphic<TReq>(),
                             private val responseSzr: ISerializer<TRes> = Polymorphic<TRes>()) : RdReactiveBase() {

    companion object : ISerializer<RdEndpoint<*,*>>{
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): RdEndpoint<*, *> = read(ctx, buffer, Polymorphic<Any?>(), Polymorphic<Any?>())

        fun <TReq, TRes> read(ctx: SerializationCtx, buffer: AbstractBuffer, requestSzr: ISerializer<TReq>, responseSzr: ISerializer<TRes>): RdEndpoint<TReq, TRes> {
            val id = RdId.read(buffer)
            return RdEndpoint<TReq, TRes>(requestSzr, responseSzr).withId(id)
        }
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: RdEndpoint<*, *>) = buffer.writeRdId(value.rdid)
    }

    private var handler: ((Lifetime, TReq) -> RdTask<TRes>)? = null

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
    fun set(handler: (TReq) -> TRes) = set ({ lf, req -> RdTask.fromResult(handler(req)) })
    constructor(handler: (TReq) -> TRes) : this () {set(handler)}


    override lateinit var serializationContext: SerializationCtx
    lateinit var lifetime: Lifetime

    override fun init(lifetime: Lifetime) {
        super.init(lifetime)

        serializationContext = super.serializationContext
        this.lifetime = lifetime

        wire.advise(lifetime, this)
    }

    override fun onWireReceived(buffer: AbstractBuffer) {
        val taskId = RdId.read(buffer)
        val value = requestSzr.read(serializationContext, buffer)
        logReceived.trace {"endpoint `$location`::($rdid) request = ${value.printToString()}"}

        //little bit monadic programming here
        Result.wrap { handler!!(lifetime, value) }
                .transform( {it}, { RdTask.faulted(it) })
                .result.advise(lifetime) { result ->
            logSend.trace { "endpoint `$location`::($rdid) response = ${result.printToString()}" }
            wire.send(rdid) { buffer ->
                taskId.write(buffer)
                RdTaskResult.write(serializationContext, buffer, result, responseSzr)
            }
        }
    }
}
