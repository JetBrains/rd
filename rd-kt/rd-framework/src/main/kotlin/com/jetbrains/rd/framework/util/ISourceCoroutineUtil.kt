package com.jetbrains.rd.framework.util

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.reactive.ISource
import com.jetbrains.rd.util.threading.coroutines.nextNotNullValueAsync
import com.jetbrains.rd.util.threading.coroutines.nextTrueValueAsync
import com.jetbrains.rd.util.threading.coroutines.nextFalseValueAsync
import com.jetbrains.rd.util.threading.coroutines.nextValue
import com.jetbrains.rd.util.threading.coroutines.nextTrueValue
import com.jetbrains.rd.util.threading.coroutines.nextFalseValue
import com.jetbrains.rd.util.threading.coroutines.nextValueAsync
import com.jetbrains.rd.util.threading.coroutines.nextNotNullValue
import com.jetbrains.rd.util.threading.coroutines.adviseSuspend
import kotlinx.coroutines.Deferred
import kotlin.coroutines.CoroutineContext

@Deprecated("Api moved to rd-core", ReplaceWith("nextValueAsync(lifetime, condition)", "com.jetbrains.rd.util.threading.coroutines.nextValueAsync"))
fun <T> ISource<T>.nextValueAsync(
    lifetime: Lifetime,
    condition: (T) -> Boolean = { true }
): Deferred<T> = nextValueAsync(lifetime, condition)

@Deprecated("Api moved to rd-core", ReplaceWith("nextNotNullValueAsync(lifetime)", "com.jetbrains.rd.util.threading.coroutines.nextNotNullValueAsync"))
fun <T : Any> ISource<T?>.nextNotNullValueAsync(lifetime: Lifetime): Deferred<T> = nextNotNullValueAsync(lifetime)

@Deprecated("Api moved to rd-core", ReplaceWith("nextValue()", "com.jetbrains.rd.util.threading.coroutines.nextValue"))
suspend fun <T> ISource<T>.nextValue(
    lifetime: Lifetime = Lifetime.Eternal,
    condition: (T) -> Boolean = { true }
): T = nextValue()


@Deprecated("Api moved to rd-core", ReplaceWith("nextTrueValueAsync(lifetime)", "com.jetbrains.rd.util.threading.coroutines.nextTrueValueAsync"))
fun ISource<Boolean>.nextTrueValueAsync(lifetime: Lifetime) = nextTrueValueAsync(lifetime)
@Deprecated("Api moved to rd-core", ReplaceWith("nextFalseValueAsync(lifetime)", "com.jetbrains.rd.util.threading.coroutines.nextFalseValueAsync"))
fun ISource<Boolean>.nextFalseValueAsync(lifetime: Lifetime) = nextFalseValueAsync(lifetime)

@Deprecated("Api moved to rd-core", ReplaceWith("nextTrueValue()", "com.jetbrains.rd.util.threading.coroutines.nextTrueValue"))
suspend fun ISource<Boolean>.nextTrueValue(lifetime: Lifetime = Lifetime.Eternal) = nextTrueValue()

@Deprecated("Api moved to rd-core", ReplaceWith("nextValue() { !it }", "com.jetbrains.rd.util.threading.coroutines.nextValue"))
suspend fun ISource<Boolean>.nextFalseValue(lifetime: Lifetime = Lifetime.Eternal) = nextFalseValue()

@Deprecated("Api moved to rd-core", ReplaceWith("nextNotNullValue()", "com.jetbrains.rd.util.threading.coroutines.nextNotNullValue"))
suspend fun <T : Any> ISource<T?>.nextNotNullValue(lifetime: Lifetime = Lifetime.Eternal): T =
    nextNotNullValue()

@Deprecated("Api moved to rd-core", ReplaceWith("adviseSuspend(lifetime, scheduler, handler)", "com.jetbrains.rd.util.threading.coroutines.adviseSuspend"))
fun<T> ISource<T>.adviseSuspend(lifetime: Lifetime, scheduler: IScheduler, handler: suspend (T) -> Unit) {
    adviseSuspend(lifetime, scheduler, handler)
}

@Deprecated("Api moved to rd-core", ReplaceWith("adviseSuspend(lifetime, context, handler)", "com.jetbrains.rd.util.threading.coroutines.adviseSuspend"))
fun<T> ISource<T>.adviseSuspend(lifetime: Lifetime, context: CoroutineContext, handler: suspend (T) -> Unit) {
    adviseSuspend(lifetime, context, handler)
}