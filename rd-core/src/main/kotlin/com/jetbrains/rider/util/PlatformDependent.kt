package com.jetbrains.rider.util

import kotlin.reflect.KClass

expect open class ExecutionException(message: String, cause: Throwable?) : Exception
expect fun currentThreadName() : String

expect class AtomicReference<T> (initial: T) {
    fun get() : T
    fun getAndUpdate(f : (T) -> T) : T
}

expect class CancellationException() : IllegalStateException

expect class ThreadLocal<T> {
    fun get() : T
    fun set(v: T)
}

expect fun <T> threadLocalWithInitial(initial: () -> T) : ThreadLocal<T>

expect val eol : String

expect object Sync {
    fun <R: Any?> lock(obj: Any, acton: () -> R) : R
    fun notifyAll(obj: Any) : Unit
    fun notify(obj: Any) : Unit
    fun wait(obj: Any) : Unit
}

expect fun<K,V> concurrentMapOf() : MutableMap<K,V>

expect interface Closeable {
    fun close()
}

expect inline fun <T : Closeable?, R> T.use(block:(T) -> R) : R

expect fun Throwable.getThrowableText(): String

expect fun qualifiedName(kclass: KClass<*>) : String