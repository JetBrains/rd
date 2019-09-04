package com.jetbrains.rd.util

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

expect interface Runnable {
    fun run()
}

expect interface Callable<T> {
    fun call() : T
}

expect fun <T> threadLocalWithInitial(initial: () -> T) : ThreadLocal<T>

expect val eol : String

expect object Sync {
    inline fun <R: Any?> lock(obj: Any, acton: () -> R) : R
    fun notifyAll(obj: Any) : Unit
    fun notify(obj: Any) : Unit
    fun wait(obj: Any) : Unit
    fun wait(obj: Any, timeout: Long) : Unit
}

expect fun<K,V> concurrentMapOf() : MutableMap<K,V>

expect interface Closeable {
    fun close()
}

expect inline fun <T : Closeable?, R> T.use(block:(T) -> R) : R

expect fun Throwable.getThrowableText(): String

expect fun qualifiedName(kclass: KClass<*>) : String

expect fun measureTimeMillis(block: () -> Unit) : Long

expect fun assert(value: Boolean)

expect inline fun assert(value: Boolean, lazyMessage: () -> Any)

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

expect class AtomicInteger constructor(v: Int = 0){
    fun get() : Int
    fun getAndAdd(v: Int) : Int
    fun incrementAndGet() : Int
    fun decrementAndGet() : Int
    fun compareAndSet(expect: Int, updated: Int) : Boolean
}

expect class Queue<E>() {
    fun offer(element: E): Boolean
    fun isEmpty(): Boolean
    fun poll(): E?
}

expect class ConcurrentHashMap<K, V>() : MutableMap<K, V> {
    fun putIfAbsent(key: K, value: V) : V?
}

expect fun printlnError(msg: String)

expect inline fun spinUntil(condition: () -> Boolean)
expect inline fun spinUntil(timeoutMs: Long, condition: () -> Boolean) : Boolean

expect abstract class EnumSet<T : Enum<T>>
expect inline fun <reified T:Enum<T>> enumSetOf(values: Set<T> = emptySet()) : EnumSet<T>
expect fun <T: Enum<T>> EnumSet<T>.values() : Set<T>