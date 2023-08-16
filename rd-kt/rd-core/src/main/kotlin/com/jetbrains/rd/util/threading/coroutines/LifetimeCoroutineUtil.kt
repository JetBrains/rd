package com.jetbrains.rd.util.threading.coroutines

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

fun LifetimeDefinition.synchronizeWith(scope: CoroutineScope, supportsTerminationUnderExecution: Boolean = false) {
    if (onTerminationIfAlive { scope.cancel() }) {
        scope.coroutineContext.job.invokeOnCompletion { terminate(supportsTerminationUnderExecution) }
    } else {
        scope.cancel()
    }
}

suspend fun waitFor(timeout: Duration, maxDelay: Long = 256, lifetime: Lifetime = Lifetime.Eternal, condition: suspend () -> Boolean): Boolean {
    val start = System.nanoTime()
    val durationNanos = timeout.toNanos()

    var ms = 16L
    while (!condition()) {
        if (lifetime.isNotAlive || System.nanoTime() - start > durationNanos)
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
) = this.coroutineScope.launch(context, start, block)

fun Lifetime.launch(
    scheduler: IScheduler,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
) = launch(scheduler.asCoroutineDispatcher, start, block)

fun <T> Lifetime.async(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T
) = this.coroutineScope.async(context, start, block)

fun <T> Lifetime.async(
    scheduler: IScheduler,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T
) = async(scheduler.asCoroutineDispatcher, start, block)

suspend fun <T> withRdSchedulerContext(scheduler: IScheduler, block: suspend CoroutineScope.() -> T): T =
    withContext(scheduler.asCoroutineDispatcher, block)


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