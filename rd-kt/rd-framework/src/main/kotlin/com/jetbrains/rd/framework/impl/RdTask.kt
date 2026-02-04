package com.jetbrains.rd.framework.impl

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.framework.util.launch
import com.jetbrains.rd.util.*
import com.jetbrains.rd.util.lifetime.*
import com.jetbrains.rd.util.reactive.*
import com.jetbrains.rd.util.reflection.threadLocal
import com.jetbrains.rd.util.string.RName
import com.jetbrains.rd.util.string.condstr
import com.jetbrains.rd.util.string.printToString
import com.jetbrains.rd.util.threading.SynchronousScheduler
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

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
    fun set(e: Throwable) = result.set(RdTaskResult.Fault(e))

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
    val wireScheduler: IScheduler
) : RdTask<TRes>(), IRdWireable {

    override val protocol: IProtocol? get() = call.protocol
    override val serializationContext: SerializationCtx? get() = call.serializationContext

    val wire = call.protocol?.wire
    override val location: RName = call.location.sub(rdid.toString(), ".")

    override fun toString(): String = this::class.simpleName + ": `$location`" + ": ($rdid)"

    override fun onWireReceived(buffer: AbstractBuffer, dispatchHelper: IRdWireableDispatchHelper) {
        val proto = protocol
        val ctx = serializationContext

        if (proto == null || ctx == null) {
            RdReactiveBase.logReceived.trace { "$this is not bound. Message for (${dispatchHelper.rdId} will not be processed" }
            return
        }


        return onWireReceived(proto, ctx, buffer, dispatchHelper)
    }

    abstract fun onWireReceived(proto: IProtocol, ctx: SerializationCtx, buffer: AbstractBuffer, dispatchHelper: IRdWireableDispatchHelper)
}

class CallSiteWiredRdTask<TReq, TRes>(
    val outerLifetime: Lifetime,
    call: RdCall<TReq, TRes>,
    rdid: RdId,
    wireScheduler: IScheduler
) : WiredRdTask<TReq, TRes>(call, rdid, wireScheduler) {

    private val taskWireSubscriptionDefinition = outerLifetime.createNested()

    init {

        wire?.advise(taskWireSubscriptionDefinition.lifetime, this) //this lifetimeDef listen only one value
        if (!taskWireSubscriptionDefinition.onTerminationIfAlive { result.setIfEmpty(RdTaskResult.Cancelled()) }) {
            result.setIfEmpty(RdTaskResult.Cancelled())
        }

        result.adviseOnce(Lifetime.Eternal) { taskResult ->
            taskWireSubscriptionDefinition.terminate() //no need to listen result or cancellation from wire

            if (taskResult is RdTaskResult.Cancelled) { //we need to transfer cancellation to the other side
                sendCancellation()
            }
        }
    }

    internal fun cancel() {
        taskWireSubscriptionDefinition.terminate()
    }

    private fun sendCancellation() {
        RdReactiveBase.logSend.trace { "send cancellation" }
        wire?.send(rdid) { writer -> writer.writeVoid(Unit) } //send cancellation to the other side
    }

    override fun onWireReceived(proto: IProtocol, ctx: SerializationCtx, buffer: AbstractBuffer, dispatchHelper: IRdWireableDispatchHelper) {
        // we are at call side, so listening no response and bind it if it's bindable
        val resultFromWire = RdTaskResult.read(ctx, buffer, call.responseSzr)
        RdReactiveBase.logReceived.trace { "call `${call.location}` (${call.rdid}) received response for task '$rdid' : ${resultFromWire.printToString()} " }

        if (result.hasValue) {
            RdReactiveBase.logReceived.trace { "`result` already has a value: ${result.valueOrNull}" }
            return
        }

        if (resultFromWire is RdTaskResult.Success && resultFromWire.value.isBindable()){
            val definition = LifetimeDefinition().apply { id = resultFromWire.value }

            try {
                definition.onTermination { sendCancellation() }
                resultFromWire.value.preBindPolymorphic(definition, call, rdid.toString())
                resultFromWire.value.bindPolymorphic()

                outerLifetime.attach(definition, true)
            } catch (e: Throwable) {
                definition.terminate()
                throw e
            }
        } else if (resultFromWire is RdTaskResult.Cancelled)
            sendCancellation()

        dispatchHelper.dispatch(wireScheduler) {
            if (!result.setIfEmpty(resultFromWire))
                RdReactiveBase.logReceived.trace { "call `${call.location}` (${call.rdid}) response was dropped, task result is: ${result.valueOrNull}" }
        }
    }
}

class EndpointWiredRdTask<TReq, TRes>(
    bindLifetime: Lifetime,
    call: RdCall<TReq, TRes>,
    rdid: RdId,
    wireScheduler: IScheduler
) : WiredRdTask<TReq, TRes>(call, rdid, wireScheduler) {

    private val def = bindLifetime.createNested().apply { id = this@EndpointWiredRdTask }
    val lifetime get() = def.lifetime

    init {
        wire?.advise(lifetime, this)
        lifetime.onTerminationIfAlive { result.setIfEmpty(RdTaskResult.Cancelled()) }

        val ctx = serializationContext
        if (ctx != null) {
            result.adviseOnce(Lifetime.Eternal) { taskResult ->
                val proto = protocol

                if (taskResult is RdTaskResult.Success && taskResult.value.isBindable()) {
                    if (proto == null) {
                        wire?.send(rdid) { writer -> RdTaskResult.write(ctx,writer, RdTaskResult.Cancelled(), call.responseSzr)}
                        return@adviseOnce
                    }

                    taskResult.value.identifyPolymorphic(proto.identity, proto.identity.next(rdid))
                    lifetime.executeIfAlive {
                        taskResult.value.preBindPolymorphic(lifetime, call, rdid.toString())
                        if (lifetime.isNotAlive)
                            return@executeIfAlive

                        wire?.send(rdid) { writer ->
                            RdTaskResult.write(ctx, writer, taskResult, call.responseSzr)
                        }

                        if (lifetime.isNotAlive)
                            return@executeIfAlive

                        taskResult.value.bindPolymorphic()
                    }
                } else {
                    def.terminate()
                    wire?.send(rdid) { writer ->
                        RdTaskResult.write(ctx, writer, taskResult, call.responseSzr)
                    }
                }
            }
        }
    }

    override fun onWireReceived(proto: IProtocol, ctx: SerializationCtx, buffer: AbstractBuffer, dispatchHelper: IRdWireableDispatchHelper) {
        //we are on endpoint side, so listening for cancellation
        RdReactiveBase.logReceived.trace { "received cancellation" }
        buffer.readVoid() //nothing just a void value

        dispatchHelper.dispatch(wireScheduler) {
            val success = result.setIfEmpty(RdTaskResult.Cancelled())
            val wireScheduler = call.protocol?.scheduler
            if (success || wireScheduler == null)
                def.terminate()
            else if (this@EndpointWiredRdTask.lifetime.isAlive)
                wireScheduler.queue { def.terminate() } // if the value is already set, it is not a cancellation scenario, but a termination
        }
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

    private var handler: ((Lifetime, TReq) -> RdTask<TRes>)? = null
    private var bindLifetime: Lifetime = Lifetime.Terminated

    private var cancellationScheduler: IScheduler? = null
    private var handlerScheduler: IScheduler? = null

    override fun preInit(lifetime: Lifetime, proto: IProtocol) {
        super.preInit(lifetime, proto)

        kotlin.assert(bindLifetime.status == LifetimeStatus.Terminated)
        bindLifetime = lifetime

        proto.wire.advise(lifetime, this)
    }

    override fun sync(request: TReq, timeouts: RpcTimeouts?) : TRes {
        val task = startInternal(Lifetime.Eternal, request, true, SynchronousScheduler)

        val effectiveTimeouts = if (respectSyncCallTimeouts) timeouts ?: RpcTimeouts.default else RpcTimeouts.infinite

        val freezeTime = measureTimeMillis {
            if (!task.wait(effectiveTimeouts.errorAwaitTimeMs) { })
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
        return startInternal(lifetime, request, false, responseScheduler)
    }

    override suspend fun startSuspending(lifetime: Lifetime, request: TReq, responseScheduler: IScheduler?): TRes {
        lifetime.usingNested { nested ->
            val scheduler = responseScheduler ?: createResponseScheduler(nested, coroutineContext)
            return startSuspendingImpl(lifetime, request, scheduler)
        }
    }

    private fun createResponseScheduler(lifetime: Lifetime, context: CoroutineContext): IScheduler {
        val protocolScheduler = protocol?.scheduler ?: return SynchronousScheduler

        if (!async)
            return protocolScheduler // to keep the order of other callbacks from the backend

        return object : IScheduler {
            private var active by threadLocal { 0 }

            override val isActive: Boolean get() = active > 0

            override val executionOrder: ExecutionOrder
                get() = ExecutionOrder.Unknown

            override fun queue(action: () -> Unit) {
                var executed = false

                fun execute() {
                    active++
                    try {
                        action()
                    } catch (e: Throwable) {
                        logSend.error(e)
                    } finally {
                        executed = true
                        active--
                    }
                }

                lifetime.launch(context) { execute() }.invokeOnCompletion {
                    if (executed) return@invokeOnCompletion

                    // if the context or lifetime has been cancelled we should execute this queued action anyway
                    protocolScheduler.queue(::execute)
                }
            }

            override fun flush() = logSend.error { "This scheduler must not be flushed" }
        }
    }

    private suspend fun startSuspendingImpl(lifetime: Lifetime, request: TReq, responseScheduler: IScheduler): TRes {
        val task = startInternal(lifetime, request, false, responseScheduler)
        return try {
            task.awaitInternal()
        } catch (e: CancellationException) {
            task.cancel() // send the cancellation to the backend if the coroutine has been cancelled
            throw e
        }
    }

    private fun startInternal(lifetime: Lifetime, request: TReq, sync: Boolean, scheduler: IScheduler?) : CallSiteWiredRdTask<TReq, TRes> {
        val proto = protocol
        val ctx = serializationContext

        if (!async)
            assertBound()

        assertThreading()

        if (proto == null || ctx == null)
            return CallSiteWiredRdTask(Lifetime.Terminated, this, RdId.Null, SynchronousScheduler)


        val taskId = proto.identity.next(RdId.Null)
        val task = createCallSite(lifetime) { callsiteLifetime ->
            CallSiteWiredRdTask(callsiteLifetime, this, taskId, scheduler ?: proto.scheduler)
        }

        task.outerLifetime.executeIfAlive {
            proto.wire.send(rdid) { buffer ->
                logSend.trace { "call `$location`::($rdid) send${sync.condstr {" SYNC"}} request '$taskId' : ${request.printToString()} " }
                taskId.write(buffer)
                requestSzr.write(ctx, buffer, request)
            }
        }

        return task
    }

    private inline fun createCallSite(
        requestLifetime: Lifetime,
        createTask: (Lifetime) -> CallSiteWiredRdTask<TReq, TRes>
    ): CallSiteWiredRdTask<TReq, TRes> {
        if (requestLifetime.isEternal)
            return createTask(bindLifetime)

        val intersectedDef = Lifetime.defineIntersection(requestLifetime, bindLifetime)
        val task = createTask(intersectedDef.lifetime)
        task.result.advise(intersectedDef.lifetime) {
            if (it !is RdTaskResult.Success || !it.value.isBindable()) {
                intersectedDef.terminate(true)
            }
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

    override fun onWireReceived(proto: IProtocol, buffer: AbstractBuffer, ctx: SerializationCtx, dispatchHelper: IRdWireableDispatchHelper) {
        val taskId = RdId.read(buffer)
        val wiredTask = EndpointWiredRdTask(dispatchHelper.lifetime, this, taskId, cancellationScheduler ?: SynchronousScheduler)
        try {
            return onWireReceived(proto, ctx, buffer, wiredTask, dispatchHelper)
        } catch (e: Throwable) {
            wiredTask.set(e)
        }
    }

    private fun onWireReceived(proto: IProtocol, ctx: SerializationCtx, buffer: AbstractBuffer, wiredRdTask: EndpointWiredRdTask<TReq, TRes>, dispatchHelper: IRdWireableDispatchHelper) {
        val externalCancellation = wiredRdTask.lifetime
        val value = requestSzr.read(ctx, buffer)
        logReceived.trace { "onWireReceived:: endpoint `$location`::($rdid) taskId=(${wiredRdTask.rdid}) request = ${value.printToString()}" }
        dispatchHelper.dispatch(handlerScheduler) {
            logReceived.trace { "dispatch:: endpoint `$location`::($rdid) taskId=(${wiredRdTask.rdid}) request = ${value.printToString()}" }

            val rdTask = try {
                val handlerLocal = handler
                if (handlerLocal == null) {
                    val message = "Handler is not set for endpoint `$location`::($rdid) taskId=($wiredRdTask.rdid) :: received request:  ${value.printToString()}"
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
                RdTask.faulted(Exception("Unexpected exception in endpoint `$location`::($rdid) taskId=($wiredRdTask.rdid)", e))
            }

            rdTask.result.advise(Lifetime.Eternal) { result ->
                try {
                    logSend.trace { "endpoint `$location`::($rdid) taskId=(${wiredRdTask.rdid}) response = ${result.printToString()}" + (handler == null).condstr { " BUT handler is NULL" } }
                    wiredRdTask.result.setIfEmpty(result)
                } catch (ex: Throwable) {
                    logSend.log(LogLevel.Error, "Problem when responding to `${wiredRdTask}`", ex)
                    wiredRdTask.result.set(RdTaskResult.Fault(ex))
                }
            }
        }
    }
}
