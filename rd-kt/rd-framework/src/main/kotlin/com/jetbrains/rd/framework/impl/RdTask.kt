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
    start(lifetime, request).result.advise(lifetime) {
        if (it is RdTaskResult.Success<TRes>) onSuccess(it.value)
    }
}


open class RdTask<T> : IRdTask<T> {
    override val result = WriteOnceProperty<RdTaskResult<T>>()
    fun set(v : T) = result.set(RdTaskResult.Success(v))

    companion object {
        fun<T> faulted(error: Throwable) = RdTask<T>().apply { this.result.set(RdTaskResult.Fault(error)) }
        fun<T> fromResult(value: T) = RdTask<T>().apply { this.set(value) }
        fun<T> canceled() = RdTask<T>().apply { this.result.set(RdTaskResult.Cancelled()) }
    }
}


@Suppress("UNCHECKED_CAST")
abstract class WiredRdTask<TReq, TRes>(
    val call: RdCall<TReq,TRes>,
    override val rdid: RdId,
    override val wireScheduler: IScheduler
) : RdTask<TRes>(), IRdWireable {

    override val isBound  get() = call.isBound
    override val protocol: IProtocol get() = call.protocol
    override val serializationContext: SerializationCtx get() = call.serializationContext

    val wire get() = call.wire
    override val location: RName = call.location.sub(rdid.toString(), ".")
}

class CallSiteWiredRdTask<TReq, TRes>(
    outerLifetime: Lifetime,
    call: RdCall<TReq, TRes>,
    rdid: RdId,
    wireScheduler: IScheduler
) : WiredRdTask<TReq, TRes>(call, rdid, wireScheduler) {

    private val taskWireSubscriptionDefinition = outerLifetime.createNested()

    init {

        call.wire.advise(taskWireSubscriptionDefinition.lifetime, this) //this lifetimeDef listen only one value
        taskWireSubscriptionDefinition.onTerminationIfAlive { result.setIfEmpty(RdTaskResult.Cancelled()) }

        result.adviseOnce(Lifetime.Eternal) { taskResult ->
            taskWireSubscriptionDefinition.terminate() //no need to listen result or cancellation from wire

            if (taskResult is RdTaskResult.Success && taskResult.value != null && taskResult.value.isBindable()) {
                taskResult.value.bindPolymorphic(outerLifetime, call, rdid.toString())
                if (!outerLifetime.onTerminationIfAlive(::sendCancellation)) {
                    sendCancellation()
                }
            } else if (taskResult is RdTaskResult.Cancelled) { //we need to transfer cancellation to the other side
                sendCancellation()
            }
        }
    }

    internal fun cancel() {
        taskWireSubscriptionDefinition.terminate()
    }

    private fun sendCancellation() {
        RdReactiveBase.logSend.trace { "send cancellation" }
        wire.send(rdid) { writer ->
            writer.writeVoid(Unit)
        } //send cancellation to the other side
    }

    override fun onWireReceived(buffer: AbstractBuffer) {
        // we are at call side, so listening no response and bind it if it's bindable
        val resultFromWire = RdTaskResult.read(call.serializationContext, buffer, call.responseSzr)

        RdReactiveBase.logReceived.trace { "call `${call.location}` (${call.rdid}) received response for task '$rdid' : ${resultFromWire.printToString()} " }
        if (!result.setIfEmpty(resultFromWire))
            RdReactiveBase.logReceived.trace { "call `${call.location}` (${call.rdid}) response was dropped, task result is: ${result.valueOrNull}" }
    }
}

class EndpointWiredRdTask<TReq, TRes>(
    bindLifetime: Lifetime,
    call: RdCall<TReq, TRes>,
    rdid: RdId,
    wireScheduler: IScheduler
) : WiredRdTask<TReq, TRes>(call, rdid, wireScheduler) {

    private val def = bindLifetime.createNested()
    val lifetime get() = def.lifetime

    init {
        call.wire.advise(lifetime, this)
        lifetime.onTerminationIfAlive { result.setIfEmpty(RdTaskResult.Cancelled()) }

        result.adviseOnce(Lifetime.Eternal) { taskResult ->
            if (taskResult is RdTaskResult.Success && taskResult.value != null && taskResult.value.isBindable()) {
                taskResult.value.identifyPolymorphic(call.protocol.identity, call.rdid.mix(rdid.toString()))
                taskResult.value.bindPolymorphic(lifetime, call, rdid.toString())
            } else {
                def.terminate()
            }

            wire.send(rdid) { writer ->
                RdTaskResult.write(call.serializationContext, writer, taskResult, call.responseSzr)
            }
        }
    }

    override fun onWireReceived(buffer: AbstractBuffer) {
        //we are on endpoint side, so listening for cancellation
        RdReactiveBase.logReceived.trace { "received cancellation" }
        buffer.readVoid() //nothing just a void value
        result.setIfEmpty(RdTaskResult.Cancelled())
        def.terminate()
    }
}


class RpcTimeouts(val warnAwaitTimeMs: Long, val errorAwaitTimeMs: Long) {
    companion object {
        private const val MaxMilliseconds = Long.MAX_VALUE / 1_000_000 // Long.MAX_VALUE nanoseconds in milliseconds

        val default = RpcTimeouts(200L, 3000L)
        val longRunning = RpcTimeouts(10000L, 15000L)
        val infinite = RpcTimeouts(60000L, MaxMilliseconds)
    }

    @Deprecated("Use property with \"Ms\" suffix", ReplaceWith("warnAwaitTimeMs"))
    val warnAwaitTime = warnAwaitTimeMs

    @Deprecated("Use property with \"Ms\" suffix", ReplaceWith("errorAwaitTimeMs"))
    val errorAwaitTime = errorAwaitTimeMs

    init {
        assert(warnAwaitTimeMs <= MaxMilliseconds) { "warnAwaitTimeMs value of $warnAwaitTimeMs is supposed to fit into Long when converted to nanoseconds" }
        assert(errorAwaitTimeMs <= MaxMilliseconds) { "errorAwaitTimeMs value of $errorAwaitTimeMs is supposed to fit into Long when converted to nanoseconds" }
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
    private var cancellationScheduler: IScheduler? = null
    private var handlerScheduler: IScheduler? = null

    override val wireScheduler get() = handlerScheduler ?: super.wireScheduler

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
            if (!task.wait(effectiveTimeouts.errorAwaitTimeMs) {
                    if (protocol.scheduler.isActive)
                        containingExt?.pumpScheduler()
                }
            )
                throw TimeoutException("Sync execution of rpc `$location` is timed out in ${effectiveTimeouts.errorAwaitTimeMs} ms")
        }
        if (freezeTime > effectiveTimeouts.warnAwaitTimeMs) logAssert.error {"Sync execution of rpc `$location` executed too long: $freezeTime ms "}
        return task.result.valueOrThrow.unwrap()

    }


    @Deprecated("Use overload with lifetime", ReplaceWith("start(/*lifetime*/, request, responseScheduler)","com.jetbrains.rd.util.lifetime.Lifetime"))
    override fun start(request: TReq, responseScheduler: IScheduler?) : IRdTask<TRes> {
        return start(Lifetime.Eternal, request, responseScheduler)
    }

    @Deprecated("Use overload with lifetime", ReplaceWith("start(/*lifetime*/, request)", "com.jetbrains.rd.util.lifetime.Lifetime") )
    fun start(request: TReq) = start(Lifetime.Eternal, request)

    fun start(lifetime: Lifetime, request: TReq) = start(lifetime, request, null)

    override fun start(lifetime: Lifetime, request: TReq, responseScheduler: IScheduler?) : IRdTask<TRes> {
        return startInternal(lifetime, request, false, responseScheduler ?: protocol.scheduler)
    }

    override suspend fun startSuspending(lifetime: Lifetime, request: TReq, responseScheduler: IScheduler?): TRes {
        val task = startInternal(lifetime, request, false, responseScheduler ?: protocol.scheduler)
        return try {
            task.awaitInternal()
        } catch (e: CancellationException) {
            task.cancel() // send the cancellation to the backend if the coroutine has been cancelled
            throw e;
        }
    }

    private fun startInternal(lifetime: Lifetime, request: TReq, sync: Boolean, scheduler: IScheduler) : CallSiteWiredRdTask<TReq, TRes> {
        assertBound()
        if (!async) assertThreading()

        val taskId = protocol.identity.next(RdId.Null)
        val task = CallSiteWiredRdTask(lifetime.intersect(bindLifetime), this, taskId, scheduler)

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
    override fun set(cancellationScheduler: IScheduler?, handlerScheduler: IScheduler?, handler: (Lifetime, TReq) -> RdTask<TRes>) {
        this.handler = handler
        this.cancellationScheduler = cancellationScheduler
        this.handlerScheduler = handlerScheduler
    }

    constructor(cancellationScheduler: IScheduler? = null, handlerScheduler: IScheduler? = null, handler:(Lifetime, TReq) -> RdTask<TRes>) : this() { set(cancellationScheduler, handlerScheduler, handler) }

    /**
     * Assigns a handler that executes the API synchronously.
     */
    constructor(cancellationScheduler: IScheduler? = null, handlerScheduler: IScheduler? = null, handler: (TReq) -> TRes) : this () { set(cancellationScheduler, handlerScheduler, handler) }

    override fun onWireReceived(buffer: AbstractBuffer) {
        val taskId = RdId.read(buffer)

        val wiredTask = EndpointWiredRdTask(bindLifetime, this, taskId, cancellationScheduler ?: SynchronousScheduler)
        val externalCancellation = wiredTask.lifetime

        val rdTask = try {
            val value = requestSzr.read(serializationContext, buffer)
            logReceived.trace { "endpoint `$location`::($rdid) taskId=($taskId) request = ${value.printToString()}" }

            val handlerLocal = handler
            if (handlerLocal == null) {
                val message = "Handler is not set for endpoint `$location`::($rdid) taskId=($taskId) :: received request:  ${value.printToString()}";
                logReceived.error { message }
                RdTask.faulted(Exception(message))
            } else {
                try {
                    handlerLocal.invoke(externalCancellation, value)
                } catch (e: Throwable) {
                    RdTask.faulted(e)
                }
            }
        } catch (e: Throwable) {
            RdTask.faulted(Exception("Unexpected exception in endpoint `$location`::($rdid) taskId=($taskId)", e))
        }

        rdTask.result.advise(Lifetime.Eternal) { result ->
            try {
                logSend.trace {"endpoint `$location`::($rdid) taskId=($taskId) response = ${result.printToString()}" + (handler == null).condstr { " BUT handler is NULL" }}
                wiredTask.result.setIfEmpty(result)
            } catch (ex: Throwable) {
                logSend.log(LogLevel.Error, "Problem when responding to `${wiredTask}`", ex)
                wiredTask.result.set(RdTaskResult.Fault(ex))
            }
        }
    }
}
