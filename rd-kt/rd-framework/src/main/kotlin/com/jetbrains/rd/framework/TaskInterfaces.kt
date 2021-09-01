package com.jetbrains.rd.framework

import com.jetbrains.rd.framework.impl.RdTask
import com.jetbrains.rd.framework.impl.RpcTimeouts
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IOptPropertyView
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.reactive.RdFault
import com.jetbrains.rd.util.reactive.hasValue
import com.jetbrains.rd.util.threading.SpinWait
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * The result of asynchronously executing a task.
 */
sealed class RdTaskResult<out T> {
    companion object {
        fun <T> read(ctx: SerializationCtx, buffer: AbstractBuffer, serializer: ISerializer<T>): RdTaskResult<T> {
            val kind = buffer.readInt()
            return when (kind) {
                0 -> Success(serializer.read(ctx, buffer))
                1 -> Cancelled()
                2 -> {
                    val reasonTypeFqn = buffer.readString()
                    val reasonMessage = buffer.readString()
                    val reasonAsText = buffer.readString()
                    Fault(RdFault(reasonTypeFqn, reasonMessage, reasonAsText))
                }
                else -> throw IllegalArgumentException("$kind")
            }
        }

        fun <T> write(ctx: SerializationCtx, buffer: AbstractBuffer, value: RdTaskResult<T>, serializer: ISerializer<T>) {
            when (value) {
                is Success -> {
                    buffer.writeInt(0)
                    serializer.write(ctx, buffer, value.value)
                }
                is Cancelled -> {
                    buffer.writeInt(1)
                }
                is Fault -> {
                    buffer.writeInt(2)
                    buffer.writeString(value.error.reasonTypeFqn)
                    buffer.writeString(value.error.reasonMessage)
                    buffer.writeString(value.error.reasonAsText)
                }
            }
        }

    }

    class Success<out T>(val value: T) : RdTaskResult<T>()
    class Cancelled<out T> : RdTaskResult<T>()
    class Fault<out T> private constructor(val error: RdFault) : RdTaskResult<T>() {
        constructor(e: Throwable) : this(e as? RdFault ?: RdFault(e))
    }

    inline fun <R> map(transform: (T) -> R): RdTaskResult<R> {
        return when (this) {
            is Success -> Success(transform(value))
            is Cancelled -> Cancelled()
            is Fault -> Fault(error)

        }
    }

    fun unwrap() : T {
        return when (this) {
            is Success -> value
            is Cancelled -> throw CancellationException("Task finished in Cancelled state")
            is Fault -> throw error
        }
    }

    override fun toString() : String =
        this::class.simpleName!! + when (this) {
            is Success -> " :: $value"
            is Cancelled -> ""
            is Fault -> " :: $error"
        }
}

/**
 * Represents a task that can be asynchronously executed.
 */
interface IRdTask<out T> {
    val result: IOptPropertyView<RdTaskResult<T>>
}

inline fun <T> IRdTask<T>.wait(timeoutMs: Long, pump: () -> Unit) : Boolean {
    return SpinWait.spinUntil(Lifetime.Eternal, timeoutMs) {
        result.hasValue.apply { if (!this) pump() }
    }
}

internal suspend fun <T> IRdTask<T>.awaitInternal(): T = Lifetime.using { lifetime ->
    suspendCancellableCoroutine { c ->
        result.advise(lifetime) {
            when (it) {
                is RdTaskResult.Success -> c.resume(it.value)
                is RdTaskResult.Cancelled -> c.cancel(CancellationException("Task finished in Cancelled state"))
                is RdTaskResult.Fault -> c.resumeWithException(it.error)
            }
        }
    }
}

val <T> IRdTask<T>.isSucceeded : Boolean get() = result.valueOrNull is RdTaskResult.Success
val <T> IRdTask<T>.isCanceled : Boolean get() = result.valueOrNull is RdTaskResult.Cancelled
val <T> IRdTask<T>.isFaulted : Boolean get() = result.valueOrNull is RdTaskResult.Fault

/**
 * Represents an API provided by the remote process which can be invoked through the protocol.
 */
interface IRdCall<in TReq, out TRes> {
    /**
     * Invokes the API with the parameters given as [request] and waits for the result.
     */
    fun sync(request: TReq, timeouts: RpcTimeouts? = null) : TRes

    /**
     * Asynchronously invokes the API with the parameters given as [request] and waits for the result.
     * The returned task will have its result value assigned through the given [responseScheduler].
     */
    @Deprecated("Use overload with Lifetime")
    fun start(request: TReq, responseScheduler: IScheduler? = null): IRdTask<TRes>
    fun start(lifetime: Lifetime, request: TReq, responseScheduler: IScheduler? = null): IRdTask<TRes>

    suspend fun startSuspending(lifetime: Lifetime, request: TReq, responseScheduler: IScheduler? = null): TRes
}

/**
 * Counterpart of IRdCall.
 */
interface IRdEndpoint<TReq, TRes> {
    /**
     * Assigns a handler that executes the API asynchronously.
     */
    fun set(cancellationScheduler: IScheduler? = null, handlerScheduler: IScheduler? = null, handler: (Lifetime, TReq) -> RdTask<TRes>)

    /**
     * Assigns a handler that executes the API synchronously.
     */
    fun set(cancellationScheduler: IScheduler? = null, handlerScheduler: IScheduler? = null, handler: (TReq) -> TRes) = set(cancellationScheduler, handlerScheduler) { _, req -> RdTask.fromResult(handler(req)) }
}