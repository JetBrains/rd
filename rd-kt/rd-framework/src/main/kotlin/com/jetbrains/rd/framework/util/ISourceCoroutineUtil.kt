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

fun <T : Any> ISource<T?>.nextNotNullValueAsync(lifetime: Lifetime): Deferred<T> = CompletableDeferred<T>().also { d ->
    val nestedDef = lifetime.createNested().apply {
        synchronizeWith(d)
    }

    advise(nestedDef.lifetime) {
        if (it != null) {
            d.complete(it)
        }
    }
}

suspend fun <T> ISource<T>.nextValue(
    lifetime: Lifetime = Lifetime.Eternal,
    condition: (T) -> Boolean = { true }
): T = lifetime.usingNested { // unsubscribe if coroutine was cancelled
    nextValueAsync(it, condition).await()
}

fun ISource<Boolean>.nextTrueValueAsync(lifetime: Lifetime) = nextValueAsync(lifetime) { it }
fun ISource<Boolean>.nextFalseValueAsync(lifetime: Lifetime) = nextValueAsync(lifetime) { !it }

suspend fun ISource<Boolean>.nextTrueValue(lifetime: Lifetime = Lifetime.Eternal) = nextValue(lifetime) { it }
suspend fun ISource<Boolean>.nextFalseValue(lifetime: Lifetime = Lifetime.Eternal) = nextValue(lifetime) { !it }

suspend fun <T : Any> ISource<T?>.nextNotNullValue(lifetime: Lifetime = Lifetime.Eternal): T =
    lifetime.usingNested { // unsubscribe if coroutine was cancelled
        nextNotNullValueAsync(lifetime).await()
    }

fun<T> ISource<T>.adviseSuspend(lifetime: Lifetime, scheduler: IScheduler, handler: suspend (T) -> Unit) {
    adviseSuspend(lifetime, scheduler.asCoroutineDispatcher(allowInlining = true), handler)
}

fun<T> ISource<T>.adviseSuspend(lifetime: Lifetime, context: CoroutineContext, handler: suspend (T) -> Unit) {
    advise(lifetime) {
        lifetime.launch(context) {
            handler(it)
        }
    }
}