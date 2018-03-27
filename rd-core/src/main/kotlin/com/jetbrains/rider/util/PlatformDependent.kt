package com.jetbrains.rider.util

import kotlin.reflect.KClass

expect open class ExecutionException(message: String, cause: Throwable?) : Exception
expect fun currentThreadName() : String

expect class AtomicReference<T> (initial: T) {
    fun get() : T
    fun getAndUpdate(f : (T) -> T) : T
}

expect class CancellationException() : IllegalStateException {
    constructor(message: String)
}
expect class TimeoutException(message: String) : Exception

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

expect fun measureTimeMillis(block: () -> Unit) : Long

//special jvm classes
expect class URI(uriString: String)

expect class Date(millisSinceEpoch: Long) {
    /**
     * Time in milliseconds since epoch
     */
    fun getTime(): Long
}

expect class UUID(hi: Long, lo: Long) {
    fun getMostSignificantBits(): Long
    fun getLeastSignificantBits(): Long
}

expect class AtomicInteger constructor(v: Int){
    fun get() : Int
    fun getAndAdd(v: Int) : Int
    fun incrementAndGet() : Int
    fun decrementAndGet() : Int
}

expect class Queue<E>() {
    fun offer(element: E): Boolean
    fun isEmpty(): Boolean
    fun poll(): E?
}
