package com.jetbrains.rider.framework.impl

import com.jetbrains.rider.framework.*
import com.jetbrains.rider.framework.base.RdReactiveBase
import com.jetbrains.rider.framework.base.printToString
import com.jetbrains.rider.framework.base.withId
import com.jetbrains.rider.util.*
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.lifetime.plusAssign
import com.jetbrains.rider.util.reactive.*
import com.jetbrains.rider.util.threading.SynchronousScheduler
import java.io.InputStream
import java.io.OutputStream
import java.rmi.RemoteException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.system.measureTimeMillis

//for polymorphic marshalling
interface IRdRpc {
//    companion object : IMarshaller<IRdRpc> {
//        override val _type: Class<*> get() = RdEndpoint::class.java
//        override fun read(ctx: SerializationCtx, stream: InputStream): IRdCall<*,*> = RdCall.read(ctx, stream)
//        override fun write(ctx: SerializationCtx, stream: OutputStream, value: IRdRpc) = RdEndpoint.write(ctx, stream, value as RdEndpoint<*, *>)
//    }
}

interface IRdTask<out T> {
    val result : IReadonlyProperty<RdTaskResult<T>>
}

val <T> IRdTask<T>.isSucceeded : Boolean get() = result.maybe.asNullable() is RdTaskResult.Success
val <T> IRdTask<T>.isCanceled : Boolean get() = result.maybe.asNullable() is RdTaskResult.Cancelled
val <T> IRdTask<T>.isFaulted : Boolean get() = result.maybe.asNullable() is RdTaskResult.Fault

fun <T> IRdTask<T>.wait(timeoutMs: Long) : Boolean {
    val evt = CountDownLatch(1)
    result.advise(Lifetime.Eternal) { evt.countDown() }
    return evt.await(timeoutMs, TimeUnit.MILLISECONDS)
}

interface IRdCall<in TReq, out TRes> : IRdRpc {
    fun sync(request: TReq, timeouts: RpcTimeouts? = null) : TRes
    fun start(request: TReq, responseScheduler: IScheduler? = null) : IRdTask<TRes>
}

fun<TReq, TRes> IRdCall<TReq, TRes>.startAndAdviseSuccess(request: TReq, onSuccess: (TRes) -> Unit) {
    startAndAdviseSuccess(Lifetime.Eternal, request, onSuccess)
}

fun<TReq, TRes> IRdCall<TReq, TRes>.startAndAdviseSuccess(lifetime: Lifetime, request: TReq, onSuccess: (TRes) -> Unit) {
    start(request).result.advise(lifetime) {
        if (it is RdTaskResult.Success<TRes>) onSuccess(it.value)
    }
}


sealed class RdTaskResult<out T> {
    companion object {
//        override val _type: Class<*> get() = RdTaskResult::class.java

        fun<T> read(ctx: SerializationCtx, stream: InputStream, serializer: ISerializer<T>): RdTaskResult<T> {
            val kind = stream.readInt()
            return when (kind) {
                0 -> Success(serializer.read(ctx, stream))
                1 -> Cancelled<T>()
                2 -> Fault<T>(RemoteException(stream.readString()))
                else -> throw IllegalArgumentException("$kind")
            }
        }

        fun<T> write(ctx: SerializationCtx, stream: OutputStream, value: RdTaskResult<T>, serializer: ISerializer<T>) {
            when (value) {
                is Success -> {
                    stream.writeInt(0)
                    serializer.write(ctx, stream, value.value)
                }
                is Cancelled -> {
                    stream.writeInt(1)
                }
                is Fault -> {
                    stream.writeInt(2)
                    var text = value.error.getThrowableText()
                    if (text.isEmpty()) text = "<empty message>"
                    stream.writeString(text)
                }
            }
        }

    }

    class Success<out T>(val value: T) : RdTaskResult<T>()
    class Cancelled<out T> : RdTaskResult<T>()
    class Fault<out T>(val error: Throwable) : RdTaskResult<T>()

    inline fun <R> map(transform: (T) -> R): RdTaskResult<R> {
        return when (this) {
            is RdTaskResult.Success -> RdTaskResult.Success(transform(value))
            is RdTaskResult.Cancelled -> RdTaskResult.Cancelled()
            is RdTaskResult.Fault -> RdTaskResult.Fault(error)
        }
    }

    fun unwrap() : T {
        return when (this) {
            is Success -> value
            is Cancelled -> throw InterruptedException("Task finished in Cancelled state")
            is Fault -> throw error
        }
    }
}

class RdTask<T> () : IRdTask<T> {
    companion object {
        fun<T> faulted(error: Throwable) = RdTask<T>().apply { this.result.set(RdTaskResult.Fault(error)) }
        fun<T> fromResult(value: T) = RdTask<T>().apply { this.set(value) }
    }

    override val result = Trigger<RdTaskResult<T>>()
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
class RdCall<TReq, TRes>(val requestSzr: ISerializer<TReq> = Polymorphic<TReq>(), val responseSzr: ISerializer<TRes> = Polymorphic<TRes>()) : RdReactiveBase(), IRdCall<TReq, TRes> {

    companion object : ISerializer<RdCall<*,*>>{
        override fun read(ctx: SerializationCtx, stream: InputStream): RdCall<*, *> = read(ctx, stream, Polymorphic<Any?>(), Polymorphic<Any?>())

        //        override val _type: Class<*> get() = throw IllegalStateException("Mustn't be used for polymorphic marshalling")
        fun <TReq, TRes> read(ctx: SerializationCtx, stream: InputStream, requestSzr: ISerializer<TReq>, responseSzr: ISerializer<TRes>): RdCall<TReq, TRes> {
            return RdCall(requestSzr, responseSzr).withId(stream.readRdId())
        }
        override fun write(ctx: SerializationCtx, stream: OutputStream, value: RdCall<*, *>) {
            stream.writeRdId(value.id)
//            throw IllegalStateException("Serialization of RdCall (${value.id}) is not allowed. The only valid option is to deserialize RdEndpoint.")
        }

        var respectSyncCallTimeouts = true
    }

    private val requests = java.util.concurrent.ConcurrentHashMap<RdId, Pair<IScheduler, RdTask<TRes>>>()

    @Volatile
    private var syncTaskId: RdId? = null


    override fun init(lifetime: Lifetime) {
        super.init(lifetime)
        wire.adviseOn(lifetime, id, SynchronousScheduler) { stream ->
            val taskId = stream.readRdId()
            val request = requests[taskId]

            if (request == null) {
                logReceived.trace { "call `${location()}` ($id) received response '$taskId' but it was dropped" }

            } else {
                val result = RdTaskResult.read(serializationContext, stream, responseSzr)
                logReceived.trace { "call `${location()}` ($id) received response '$taskId' : ${result.printToString()} " }

                val (scheduler, task) = request
                scheduler.queue {
                    if (task.result.hasValue) {
                        logReceived.trace { "call `${location()}` ($id) response was dropped, task result is: ${task.result.value}" }
                        if (isBound && defaultScheduler.isActive && requests.containsKey(taskId)) logAssert.error ( "MainThread: ${defaultScheduler.isActive}, taskId=$taskId ")
                    } else {
                        //todo could be race condition in sync mode in case of Timeout, but it's not really valid case
                        //todo but now we could start task on any scheduler - need interlocks in property
                        task.result.set(result)
                        requests.remove(taskId)
                    }
                }
            }
        }

        //cancel
        lifetime += {
            requests.forEach {
                val task = it.value.second
                task.result.set(RdTaskResult.Cancelled()) }
            requests.clear()
        }
    }


    override fun sync(request: TReq, timeouts: RpcTimeouts?) : TRes {
        try {
            val task = startInternal(request, true, SynchronousScheduler)

            val effectiveTimeouts = if (respectSyncCallTimeouts) timeouts ?: RpcTimeouts.default else RpcTimeouts.maximal

            val freezeTime = measureTimeMillis {
                if (!task.wait(effectiveTimeouts.errorAwaitTime))
                    throw TimeoutException("Sync execution of rpc `${location()}` is timed out in ${effectiveTimeouts.errorAwaitTime} ms")
            }
            if (freezeTime > effectiveTimeouts.warnAwaitTime) logAssert.error("Sync execution of rpc `${location()}` executed too long: $freezeTime ms ")
            return (task.result.value as RdTaskResult<TRes>).unwrap()
        } finally {
            syncTaskId = null
        }
    }




    override fun start(request: TReq, responseScheduler: IScheduler?) : IRdTask<TRes> {
        return startInternal(request, false, responseScheduler?:protocol.scheduler) as RdTask<TRes>
    }

    fun start(request: TReq) = start(request, null)


    private fun startInternal(request: TReq, sync: Boolean, scheduler: IScheduler) : IRdTask<*> {
        assertBound()
        if (!async) assertThreading()

        val taskId = protocol.identity.next()
        val task = RdTask<TRes>()
        requests.putUnique(taskId, scheduler to task)
        if (sync) {
            if (syncTaskId != null) throw IllegalStateException("Already exists sync task for call `${location()}`, taskId = $syncTaskId")
            syncTaskId = taskId
        }

        wire.send(id) { stream ->
            logSend.trace { "call `${location()}`::($id) send${sync.condstr {" SYNC"}} request '$taskId' : ${request.printToString()} " }
            taskId.write(stream)
            requestSzr.write(serializationContext, stream, request)
        }

        return task
    }
}



class RdEndpoint<TReq, TRes>(val requestSzr: ISerializer<TReq> = Polymorphic<TReq>(), val responseSzr: ISerializer<TRes> = Polymorphic<TRes>()) : RdReactiveBase(), IRdRpc {

    companion object : ISerializer<RdEndpoint<*,*>>{
        override fun read(ctx: SerializationCtx, stream: InputStream): RdEndpoint<*, *> = read(ctx, stream, Polymorphic<Any?>(), Polymorphic<Any?>())

        fun <TReq, TRes> read(ctx: SerializationCtx, stream: InputStream,  requestSzr: ISerializer<TReq>, responseSzr: ISerializer<TRes>): RdEndpoint<TReq, TRes> {
            val id = RdId.read(stream)
            return RdEndpoint<TReq, TRes>(requestSzr, responseSzr).withId(id)
        }
        override fun write(ctx: SerializationCtx, stream: OutputStream, value: RdEndpoint<*, *>) = stream.writeRdId(value.id)
    }

    private var handler: ((Lifetime, TReq) -> RdTask<TRes>)? = null

    fun set(handler:(Lifetime, TReq) -> RdTask<TRes>) {
        require(this.handler == null) {"handler is set already"}
        this.handler = handler
    }
    constructor(handler:(Lifetime, TReq) -> RdTask<TRes>) : this() { set(handler) }

    fun set(handler: (TReq) -> TRes) = set ({ lf, req -> RdTask.fromResult(handler(req)) })
    constructor(handler: (TReq) -> TRes) : this () {set(handler)}


    override fun init(lifetime: Lifetime) {
        super.init(lifetime)

        val serializationContext = serializationContext

        wire.advise(lifetime, id, { stream ->
            val taskId = RdId.read(stream)
            val value = requestSzr.read(serializationContext, stream)
            logReceived.trace {"endpoint `${location()}`::($id) request = ${value.printToString()}"}

            //little bit monadic programming here
            Result.wrap { handler!!(lifetime, value) }
                .transform( {it}, { RdTask.faulted(it) })
                .result.advise(lifetime) { result ->
                    logSend.trace { "endpoint `${location()}`::($id) response = ${result.printToString()}" }
                    wire.send(id) { stream ->
                        taskId.write(stream)
                        RdTaskResult.write(serializationContext, stream, result, responseSzr)
                    }
                }
        })
    }
}
