package com.jetbrains.rd.framework.util

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.threading.coroutines.*
import com.jetbrains.rd.util.threading.coroutines.launch
import com.jetbrains.rd.util.threading.coroutines.synchronizeWith
import com.jetbrains.rd.util.threading.coroutines.createTerminatedAfter
import com.jetbrains.rd.util.threading.coroutines.lifetimedCoroutineScope
import kotlinx.coroutines.*
import java.time.Duration
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext


@Deprecated("Api moved to rd-core", ReplaceWith("synchronizeWith(job, supportsTerminationUnderExecution)", "com.jetbrains.rd.util.threading.coroutines.synchronizeWith"))
fun LifetimeDefinition.synchronizeWith(job: Job, supportsTerminationUnderExecution: Boolean = false) {
    synchronizeWith(job, supportsTerminationUnderExecution)
}

@Deprecated("Api moved to rd-core", ReplaceWith("synchronizeWith(scope, supportsTerminationUnderExecution)", "com.jetbrains.rd.util.threading.coroutines.synchronizeWith"))
fun LifetimeDefinition.synchronizeWith(scope: CoroutineScope, supportsTerminationUnderExecution: Boolean = false) {
    synchronizeWith(scope, supportsTerminationUnderExecution)
}

@Deprecated("Api moved to rd-core", ReplaceWith("waitFor(timeout, maxDelay, this, condition)", "com.jetbrains.rd.util.threading.coroutines.waitFor"))
suspend fun Lifetime.waitFor(timeout: Duration, maxDelay: Long = 256, condition: suspend () -> Boolean): Boolean {
   return waitFor(timeout, maxDelay, this, condition)
}

@Deprecated("Api moved to rd-core", ReplaceWith("launch(context, start, block)", "com.jetbrains.rd.util.threading.coroutines.launch"))
fun Lifetime.launch(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
) = launch(context, start, block)

@Deprecated("Api moved to rd-core", ReplaceWith("launch(context, start, block)", "com.jetbrains.rd.util.threading.coroutines.launch"))
fun Lifetime.launch(
    scheduler: IScheduler,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
) = launch(scheduler, start, block)

@Deprecated("Api renamed and moved to rd-core", ReplaceWith("async(context, start, block)", "com.jetbrains.rd.util.threading.coroutines.async"))
    fun <T> Lifetime.startAsync(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T
) = async(context, start, block)

fun <T> Lifetime.startAsync(
    scheduler: IScheduler,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T
) = async(scheduler, start, block)

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

@Deprecated("Consider overload without lifetime, if you need to limit lifetime of the scope use lifetimedCoroutineScope", ReplaceWith("lifetimedCoroutineScope(lifetime) { withContext(context, block) }", "kotlinx.coroutines.withContext"))
suspend fun <T> withContext(lifetime: Lifetime, context: CoroutineContext, block: suspend CoroutineScope.() -> T): T =
    lifetimedCoroutineScope(lifetime) {
        withContext(context, block)
    }

@Deprecated("Use withRdSchedulerContext", ReplaceWith("withRdSchedulerContext(scheduler, block)", "com.jetbrains.rd.util.threading.coroutines.withRdSchedulerContext"))
suspend fun <T> withContext(scheduler: IScheduler, block: suspend CoroutineScope.() -> T): T =
    withRdSchedulerContext(scheduler, block)

@Deprecated("Consider using withRdSchedulerContext method, if you need to limit lifetime of the scope use lifetimedCoroutineScope", ReplaceWith("lifetimedCoroutineScope(lifetime) { withRdSchedulerContext(scheduler, block) }", "com.jetbrains.rd.util.threading.coroutines.withRdSchedulerContext"))
suspend fun <T> withContext(lifetime: Lifetime, scheduler: IScheduler, block: suspend CoroutineScope.() -> T): T =
    lifetimedCoroutineScope(lifetime) {
        withRdSchedulerContext(scheduler, block)
    }

@Deprecated("Api moved to rd-core", ReplaceWith("createTerminatedAfter(duration, terminationContext)", "com.jetbrains.rd.util.threading.coroutines.createTerminatedAfter"))
fun Lifetime.createTerminatedAfter(duration: Duration, terminationContext: CoroutineContext): Lifetime =
    createTerminatedAfter(duration, terminationContext)


/**
 * Creates a [coroutineScope] that will be cancelled on the passed lifetime termination
 **/
@Deprecated("Api moved to rd-core", ReplaceWith("lifetimedCoroutineScope(lifetime, action)", "com.jetbrains.rd.util.threading.coroutines.lifetimedCoroutineScope"))
suspend fun <T> lifetimedCoroutineScope(lifetime: Lifetime, action: suspend CoroutineScope.() -> T) = lifetimedCoroutineScope(lifetime, action)