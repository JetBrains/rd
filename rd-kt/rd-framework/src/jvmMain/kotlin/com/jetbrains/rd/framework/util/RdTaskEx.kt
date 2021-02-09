package com.jetbrains.rd.framework.util

import com.jetbrains.rd.framework.IRdTask
import com.jetbrains.rd.framework.RdTaskResult
import com.jetbrains.rd.framework.awaitInternal
import com.jetbrains.rd.framework.impl.RdTask
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.adviseOnce
import kotlinx.coroutines.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.CompletionStage
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

fun <T> Deferred<T>.toRdTask() = RdTask<T>().also { rdTask ->
    invokeOnCompletion {
        when (it) {
            null -> rdTask.set(getCompleted())
            is CancellationException -> rdTask.result.set(RdTaskResult.Cancelled())
            else -> rdTask.result.set(RdTaskResult.Fault(it))
        }
    }
}

fun <T> CompletionStage<T>.toRdTask() = RdTask<T>().also { rdTask ->
    whenComplete { res, e ->
        when (e) {
            null -> rdTask.set(res)

            is CancellationException -> rdTask.result.set(RdTaskResult.Cancelled())
            is CompletionException -> when (val unwrappedException = e.unwrap()) {
                is CancellationException -> rdTask.result.set(RdTaskResult.Cancelled())
                else -> rdTask.result.set(RdTaskResult.Fault(unwrappedException))
            }
            else -> rdTask.result.set(RdTaskResult.Fault(e))
        }
    }
}

@Deprecated("Use startSuspending instead of start().await()")
suspend fun <T> IRdTask<T>.await() = awaitInternal()

fun <T> IRdTask<T>.asCompletableFuture() = CompletableFuture<T>().also { future ->
    result.adviseOnce(Lifetime.Eternal) {
        when (it) {
            is RdTaskResult.Success -> future.complete(it.value)
            is RdTaskResult.Cancelled -> future.cancel(true)
            is RdTaskResult.Fault -> future.completeExceptionally(it.error)
        }
    }
}

private suspend fun <T> CompletableFuture<T>.await() = suspendCancellableCoroutine<T> { c ->
    whenComplete { res, e ->
        when {
            e == null -> c.resume(res)
            isCancelled -> c.cancel(e)
            else -> c.resumeWithException(e)
        }
    }
}

@JvmName("awaitAllFutures")
private suspend fun <T> Collection<CompletableFuture<T>>.awaitAll(): List<T> {
    val array = toTypedArray()
    CompletableFuture.allOf(*array).await()
    return array.map { it.getNow(null) }
}

suspend fun <T> Collection<IRdTask<T>>.awaitAll() = map { it.asCompletableFuture() }.awaitAll()
