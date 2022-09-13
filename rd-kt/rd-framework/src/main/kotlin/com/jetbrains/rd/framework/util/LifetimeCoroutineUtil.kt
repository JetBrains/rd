package com.jetbrains.rd.framework.util

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.lifetime.isEternal
import com.jetbrains.rd.util.lifetime.isNotAlive
import com.jetbrains.rd.util.reactive.IScheduler
import kotlinx.coroutines.*
import java.time.Duration
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext


fun LifetimeDefinition.synchronizeWith(job: Job, supportsTerminationUnderExecution: Boolean = false) {
    if (onTerminationIfAlive { job.cancel() }) {
        job.invokeOnCompletion { terminate(supportsTerminationUnderExecution) }
    } else {
        job.cancel()
    }
}

suspend fun Lifetime.waitFor(timeout: Duration, maxDelay: Long = 256, condition: suspend () -> Boolean): Boolean {
    val start = System.nanoTime()
    val durationNanos = timeout.toNanos()

    var ms = 16L
    while (!condition()) {
        if (isNotAlive || System.nanoTime() - start > durationNanos)
            return false

        delay(ms)
        if (ms < maxDelay) ms *= 2
    }

    return true
}

fun Lifetime.launch(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
) = RdCoroutineScope.current.launch(this, context, start, block)

fun Lifetime.launch(
    scheduler: IScheduler,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
) = launch(scheduler.asCoroutineDispatcher, start, block)

fun <T> Lifetime.startAsync(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T
) = RdCoroutineScope.current.async(this, context, start, block)

fun <T> Lifetime.startAsync(
    scheduler: IScheduler,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T
) = startAsync(scheduler.asCoroutineDispatcher, start, block)

@Deprecated("Use launch without lifetime or lifetimedCoroutineScope")
fun CoroutineScope.launchChild(
    lifetime: Lifetime,
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
) = launch(context, start, block).also { job -> lifetime.createNested().synchronizeWith(job) }

@Deprecated("Use launch method without lifetime or lifetimedCoroutineScope")
fun CoroutineScope.launchChild(
    lifetime: Lifetime,
    scheduler: IScheduler,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
) = launchChild(lifetime, scheduler.asCoroutineDispatcher, start, block)

@Deprecated("Use async without lifetime or lifetimedCoroutineScope")
fun <T> CoroutineScope.startChildAsync(
    lifetime: Lifetime,
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T
) = async(context, start, block).also { job -> lifetime.createNested().synchronizeWith(job) }

@Deprecated("Use async without lifetime or lifetimedCoroutineScope")
fun <T> CoroutineScope.startChildAsync(
    lifetime: Lifetime,
    scheduler: IScheduler,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T
) = startChildAsync(lifetime, scheduler.asCoroutineDispatcher, start, block)

suspend fun <T> withContext(lifetime: Lifetime, context: CoroutineContext, block: suspend CoroutineScope.() -> T): T =
    lifetimedCoroutineScope(lifetime) {
        withContext(context, block)
    }

suspend fun <T> withContext(scheduler: IScheduler, block: suspend CoroutineScope.() -> T): T =
    withContext(scheduler.asCoroutineDispatcher, block)

suspend fun <T> withContext(lifetime: Lifetime, scheduler: IScheduler, block: suspend CoroutineScope.() -> T): T =
    withContext(lifetime, scheduler.asCoroutineDispatcher, block)


fun Lifetime.createTerminatedAfter(duration: Duration, terminationContext: CoroutineContext): Lifetime =
    createNested().also { nested ->
        nested.launch(terminationContext, CoroutineStart.UNDISPATCHED) {
            delay(duration.toMillis())
            nested.terminate()
        }
    }


/**
 * Creates a [coroutineScope] that will be cancelled on the passed lifetime termination
 **/
suspend fun <T> lifetimedCoroutineScope(lifetime: Lifetime, action: suspend CoroutineScope.() -> T) = coroutineScope {
    if (!lifetime.isEternal)
        lifetime.createNested().synchronizeWith(coroutineContext[Job]!!)

    action()
}