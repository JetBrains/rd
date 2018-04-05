package com.jetbrains.rider.util

import com.jetbrains.rider.util.collections.QueueImpl
import kotlin.Exception
import kotlin.reflect.KClass

actual open class ExecutionException actual constructor (message: String, cause: Throwable?) : Exception(message, cause)

actual fun currentThreadName() = "main"

actual class AtomicReference<T> actual constructor(initial: T) {
    private var impl : T = initial
    actual fun get(): T = impl
    actual fun getAndUpdate(f: (T) -> T): T = impl.also { impl = f(impl) }
}

actual class CancellationException(message: String, cause: Throwable?) : IllegalStateException(message, cause) {
    actual constructor() : this("", null)
    actual constructor(message: String) : this(message, null)

}

actual class TimeoutException actual constructor(message: String) : Exception(message)

actual class ThreadLocal<T>(private var value: T) {
    actual fun get(): T = value

    actual fun set(v: T) {
        value = v
    }
}

actual fun <T> threadLocalWithInitial(initial: () -> T)= ThreadLocal(initial())

actual val eol : String = "\n"

actual object Sync {
    actual inline fun <R: Any?> lock(obj: Any, acton: () -> R) = acton()
    actual fun notifyAll(obj: Any) {}
    actual fun notify(obj: Any) {}
    actual fun wait(obj: Any) {}
}

actual fun<K,V> concurrentMapOf() = mutableMapOf<K,V>()

actual interface Closeable {
    actual fun close()
}

actual inline fun <T : Closeable?, R> T.use(block:(T) -> R) : R {
    try {
        return block(this)
    } finally {
        this?.close()
    }
}

actual fun Throwable.getThrowableText(): String = toString()

actual fun qualifiedName(kclass: KClass<*>) : String = kclass.simpleName?:"<anonymous>"

//todo js measure time
actual fun measureTimeMillis(block: () -> Unit): Long {
    getLogger("rd").warn {"Method `measureTimeMillis` always returns 0 in kotlin-js"}
    block()
    return 0
}

//special jvm classes
actual class URI actual constructor(private val uriString: String) {
    override fun toString(): String = uriString
}


actual class Date actual constructor(private val millisSinceEpoch: Long) {
    actual fun getTime(): Long = millisSinceEpoch
}

actual class UUID actual constructor(private val hi: Long, private val lo: Long) {
    actual fun getMostSignificantBits(): Long = hi
    actual fun getLeastSignificantBits(): Long = lo
}

actual class AtomicInteger actual constructor(var v: Int) {
    actual fun getAndAdd(v: Int) : Int = this.v.also { this.v += v }

    actual fun get(): Int = v

    actual fun incrementAndGet(): Int = ++v

    actual fun decrementAndGet(): Int = --v
}

actual typealias Queue<E> = QueueImpl<E>
