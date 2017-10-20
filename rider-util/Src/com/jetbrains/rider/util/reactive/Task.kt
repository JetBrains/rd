package com.jetbrains.rider.util.reactive

import com.jetbrains.rider.util.Result
import com.jetbrains.rider.util.asNullable
import com.jetbrains.rider.util.lifetime.Lifetime
import java.time.Duration
import java.util.concurrent.TimeoutException


sealed class TaskResult<out T> {

    companion object {
        fun <T> from(handler: () -> T) : TaskResult<T> {
            return try {
                TaskResult.Success(handler())
            } catch (e: InterruptedException) {
                TaskResult.Cancelled()
            } catch (e: Throwable) {
                TaskResult.Fault(e)
            }
        }
    }

    class Success<out T>(val value: T) : TaskResult<T>()
    class Cancelled<out T> : TaskResult<T>()
    class Fault<out T>(val error: Throwable) : TaskResult<T>()

    inline fun <R> map(transform: (T) -> R): TaskResult<R> {
        return when (this) {
            is Success -> Success(transform(value))
            is Cancelled -> Cancelled()
            is Fault -> Fault(error)
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




typealias Task<T> = ITrigger<TaskResult<T>>
typealias TaskHandler<TReq, TRes> = (Lifetime, TReq) -> Task<TRes>

val <T> Task<T>.isSucceeded : Boolean get() = valueOrNull is TaskResult.Success
val <T> Task<T>.isCanceled : Boolean get() = valueOrNull is TaskResult.Cancelled
val <T> Task<T>.isFaulted : Boolean get() = valueOrNull is TaskResult.Fault


fun<TReq, TRes> TaskHandler(handler: (TReq) -> Task<TRes>) : TaskHandler<TReq, TRes> = {_, req -> handler(req)}
fun<TReq, TRes> SynchronousTaskHandler(handler: (TReq) -> TRes) : TaskHandler<TReq, TRes> = {_, req -> Trigger(TaskResult.Success(handler(req)))}
fun<TReq, TRes> AlwaysFaultHandler() : TaskHandler<TReq, TRes> = {_, _ -> Trigger(TaskResult.Fault<TRes>(IllegalStateException("No valid TaskHandler specified")))}

fun<T> Task<T>.result() : T  {
    require(wait()) { "Wait mustn't interrupt" }
    return value.unwrap()
}



interface ICall<in TReq, TRes> {

    fun start(cancellationToken: Lifetime, request: TReq) : Task<TRes>

    fun sync(cancellationToken: Lifetime, request: TReq, timeout: Duration) : TRes {
        val task = start(cancellationToken, request)

        if (task.wait(cancellationToken, timeout))
            return task.value.unwrap()
        else
            throw TimeoutException()
    }
}

fun <TReq, TRes> ICall<TReq, TRes>.start(request: TReq) = start(Lifetime.Eternal, request)
fun <TReq, TRes> ICall<TReq, TRes>.sync(request: TReq, timeout: Duration) = sync(Lifetime.Eternal, request, timeout)

interface IEndpoint<out TReq, TRes> {
    fun set(handler: TaskHandler<TReq, TRes>)
}

interface ITaskFactory<TReq, TRes> : ICall<TReq, TRes>, IEndpoint<TReq, TRes>

//Implementation
class TaskFactory<TReq, TRes>(defaultHandler: TaskHandler<TReq, TRes>) : ITaskFactory<TReq, TRes> {

    constructor() : this(AlwaysFaultHandler())

    var handler: TaskHandler<TReq, TRes>

    override fun start(cancellationToken: Lifetime, request: TReq): Task<TRes> {
        if (cancellationToken.isTerminated) return Trigger(TaskResult.Cancelled())
        else
            try {
                return handler(cancellationToken, request)
            } catch (e: Throwable) {
                return Trigger(TaskResult.Fault(e))
            }
    }

    override fun set(handler: TaskHandler<TReq, TRes>) {
        this.handler = handler
    }

    init {
        this.handler = defaultHandler
    }
}


