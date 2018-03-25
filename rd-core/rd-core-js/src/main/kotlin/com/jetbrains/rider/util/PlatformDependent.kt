package com.jetbrains.rider.util

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
}

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

