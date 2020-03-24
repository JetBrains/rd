package com.jetbrains.rd.framework.impl

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.util.*
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.intersect
import com.jetbrains.rd.util.reactive.*
import com.jetbrains.rd.util.string.RName
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
        fun<T> canceled() = RdTask<T>().apply { this.result.set(RdTaskResult.Cancelled()) }
    }
}


@Suppress("UNCHECKED_CAST")
class WiredRdTask<TReq, TRes>(
    val call: RdCall<TReq,TRes>,
    override val rdid: RdId,
    override val wireScheduler: IScheduler,
    private val isEndpoint: Boolean
) : RdTask<TRes>(), IRdWireable {

    override val protocol: IProtocol
        get() = call.protocol
    override val serializationContext: SerializationCtx
        get() = call.serializationContext

    val wire get() = call.wire
    override val location: RName = call.location.sub(rdid.toString(), ".")

    internal fun subscribe(outerLifetime: Lifetime): Lifetime {
        val taskWireSubscriptionDefinition = outerLifetime.createNested()
        val externalCancellation = outerLifetime.createNested()

        call.wire.advise(taskWireSubscriptionDefinition.lifetime, this)
        taskWireSubscriptionDefinition.onTerminationIfAlive { result.setIfEmpty(RdTaskResult.Cancelled()) }

        result.adviseOnce(Lifetime.Eternal) { taskResult ->
            try {
                if (taskResult is RdTaskResult.Success && taskResult.value != null && taskResult.value.isBindable()) {
                    if (isEndpoint)
                        taskResult.value.identifyPolymorphic(call.protocol.identity, call.rdid.mix(rdid.toString()))

                    taskResult.value.bindPolymorphic(externalCancellation.lifetime, call, rdid.toString())
                }

                if (isEndpoint) {
                    if (taskResult is RdTaskResult.Cancelled) {
                        externalCancellation.terminate()
                    }

                    wire.send(rdid) { writer ->
                        RdTaskResult.write(call.serializationContext, writer, taskResult, call.responseSzr)
                    }
                } else if (taskResult is RdTaskResult.Cancelled) //we need to transfer cancellation to the other side
                {
                    RdReactiveBase.logSend.trace { "send cancellation" }
                    wire.send(rdid) { writer ->
                        writer.writeVoid(Unit)
                    } //send cancellation to the other side
                }

            } finally {
                taskWireSubscriptionDefinition.terminate() //no need to listen result or cancellation from wire
            }
        }
        
        return externalCancellation.lifetime
    }

    override fun onWireReceived(buffer: AbstractBuffer) {
        if (isEndpoint) //we are on endpoint side, so listening for cancellation
        {
            RdReactiveBase.logReceived.trace { "received cancellation" }
            buffer.readVoid() //nothing just a void value
            result.setIfEmpty(RdTaskResult.Cancelled())
        } else // we are at call side, so listening no response and bind it if it's bindable
        {
            val resultFromWire = RdTaskResult.read(call.serializationContext, buffer, call.responseSzr)

            RdReactiveBase.logReceived.trace { "call `${call.location}` (${call.rdid}) received response for task '$rdid' : ${resultFromWire.printToString()} " }
            if (!result.setIfEmpty(resultFromWire))
                RdReactiveBase.logReceived.trace { "call `${call.location}` (${call.rdid}) response was dropped, task result is: ${result.valueOrNull}" }
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





typealias RdEndpoint<TReq, TRes> = RdCall<TReq, TRes>
@Suppress("UNCHECKED_CAST", "UNUSED_PARAMETER")
//Can't be constructed by constructor, only by deserializing counterpart: RdEndpoint
class RdCall<TReq, TRes>(internal val requestSzr: ISerializer<TReq> = Polymorphic<TReq>(),
                         internal val responseSzr: ISerializer<TRes> = Polymorphic<TRes>()) : RdReactiveBase(), IRdCall<TReq, TRes>, IRdEndpoint<TReq, TRes> {

    override fun deepClone(): IRdBindable = RdCall(requestSzr, responseSzr)

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

    private var handler: ((Lifetime, TReq) -> RdTask<TRes>)? = null

    lateinit var bindLifetime : Lifetime
    private var endpointSchedulerForHandlerAndCancellation: IScheduler? = null

    override fun init(lifetime: Lifetime) {
        super.init(lifetime)
        bindLifetime = lifetime

        //Because we advise on Synchronous Scheduler: RIDER-10986
        serializationContext = super.serializationContext
        wire.advise(lifetime, this)
    }

    override fun sync(request: TReq, timeouts: RpcTimeouts?) : TRes {
        val task = startInternal(Lifetime.Eternal, request, true, SynchronousScheduler)

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
        return startInternal(Lifetime.Eternal, request, false, responseScheduler ?: protocol.scheduler) as RdTask<TRes>
    }

    fun start(request: TReq) = start(request, null)

    override fun start(lifetime: Lifetime, request: TReq, responseScheduler: IScheduler?) : IRdTask<TRes> {
        return startInternal(lifetime, request, false, responseScheduler ?: protocol.scheduler)
    }

    private fun startInternal(lifetime: Lifetime, request: TReq, sync: Boolean, scheduler: IScheduler) : IRdTask<TRes> {
        assertBound()
        if (!async) assertThreading()

        val taskId = protocol.identity.next(RdId.Null)
        val task = WiredRdTask(this, taskId, scheduler, false)

        //no need for cancellationLifetime on call site
        task.subscribe(lifetime.intersect(bindLifetime))

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
    override fun set(cancellationAndRequestScheduler: IScheduler?, handler: (Lifetime, TReq) -> RdTask<TRes>) {
        this.handler = handler
        this.endpointSchedulerForHandlerAndCancellation = cancellationAndRequestScheduler
    }

    constructor(cancellationAndRequestScheduler: IScheduler? = null, handler:(Lifetime, TReq) -> RdTask<TRes>) : this() { set(cancellationAndRequestScheduler, handler) }

    /**
     * Assigns a handler that executes the API synchronously.
     */
    constructor(cancellationAndRequestScheduler: IScheduler? = null, handler: (TReq) -> TRes) : this () { set(cancellationAndRequestScheduler, handler) }

    override fun onWireReceived(buffer: AbstractBuffer) {
        val taskId = RdId.read(buffer)

        val wiredTask = WiredRdTask(this, taskId, endpointSchedulerForHandlerAndCancellation ?: wireScheduler, true)
        val externalCancellation = wiredTask.subscribe(bindLifetime)

        val rdTask = try {
            val value = requestSzr.read(serializationContext, buffer)
            logReceived.trace { "endpoint `$location`::($rdid) taskId=($taskId) request = ${value.printToString()}" }
            handler!!.invoke(externalCancellation, value)
        } catch (e: Exception) {
            RdTask.faulted<TRes>(e)
        }

        rdTask.result.advise(Lifetime.Eternal) { result ->
            try {
                logSend.trace {"endpoint `$location`::($rdid) taskId=($taskId) response = ${result.printToString()}" + (handler == null).condstr { " BUT handler is NULL" }}
                wiredTask.result.setIfEmpty(result)
            } catch (ex: Exception) {
                logSend.log(LogLevel.Error, "Problem when responding to `${wiredTask}`", ex)
                wiredTask.result.set(RdTaskResult.Fault(ex))
            }
        }
    }
}
