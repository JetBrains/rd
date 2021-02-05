package com.jetbrains.rd.framework.util

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.reactive.ISource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

fun <T> ISource<T>.nextValueAsync(
    lifetime: Lifetime,
    condition: (T) -> Boolean = { true }
): Deferred<T> = CompletableDeferred<T>().also { d ->
    val nestedDef = lifetime.createNested().apply {
        synchronizeWith(d)
    }

    advise(nestedDef.lifetime) {
        if (condition(it)) {
            d.complete(it)
        }
    }
}

fun ISource<Boolean>.nextTrueValueAsync(lifetime: Lifetime) = nextValueAsync(lifetime) { it }
fun ISource<Boolean>.nextFalseValueAsync(lifetime: Lifetime) = nextValueAsync(lifetime) { !it }

fun <T> ISource<T>.nextNotNullValueAsync(lifetime: Lifetime) = nextValueAsync(lifetime) { it != null }

suspend fun <T> ISource<T>.nextValue(lifetime: Lifetime, condition: (T) -> Boolean = { true }) = nextValueAsync(lifetime, condition).await()

suspend fun ISource<Boolean>.nextTrueValue(lifetime: Lifetime) = nextTrueValueAsync(lifetime).await()
suspend fun ISource<Boolean>.nextFalseValue(lifetime: Lifetime) = nextFalseValueAsync(lifetime).await()

suspend fun <T> ISource<T>.nextNotNullValue(lifetime: Lifetime) = nextNotNullValueAsync(lifetime).await()

fun<T> ISource<T>.adviseSuspend(lifetime: Lifetime, scheduler: IScheduler, handler: suspend (T) -> Unit) {
    adviseSuspend(lifetime, scheduler.asCoroutineDispatcher(allowInlining = true), handler)
}

fun<T> ISource<T>.adviseSuspend(lifetime: Lifetime, context: CoroutineContext, handler: suspend (T) -> Unit) {
    advise(lifetime) {
        RdCoroutineScope.current.launch(context) {
            handler(it)
        }
    }
}