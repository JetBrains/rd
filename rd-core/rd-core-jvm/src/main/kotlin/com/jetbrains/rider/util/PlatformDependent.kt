package com.jetbrains.rider.util

import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.use

actual typealias ExecutionException = ExecutionException

actual fun currentThreadName() = Thread.currentThread().name!!

actual class AtomicReference<T> actual constructor(initial: T) {
    private val impl = AtomicReference(initial)
    actual fun get(): T = impl.get()
    actual fun getAndUpdate(f: (T) -> T): T = impl.getAndUpdate(f)
    fun getAndSet(new: T): T = impl.getAndSet(new)
}

actual typealias CancellationException = CancellationException

actual typealias ThreadLocal<T> = java.lang.ThreadLocal<T>

actual fun <T> threadLocalWithInitial(initial: () -> T) : ThreadLocal<T> = ThreadLocal.withInitial(initial)

actual fun lineSeparator() : String = System.lineSeparator()

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
actual object Sync {
    actual inline fun <R: Any?> lock(obj: Any, acton: () -> R) = synchronized(obj, acton)
    actual fun notifyAll(obj: Any) = (obj as Object).notifyAll()
    actual fun notify(obj: Any) = (obj as Object).notify()
    actual fun wait(obj: Any) = (obj as Object).wait()
}

actual fun<K,V> concurrentMapOf() : MutableMap<K,V> = ConcurrentHashMap()

actual typealias Closeable = java.io.Closeable
actual inline fun <T : Closeable?, R> T.use(block:(T) -> R) : R = use(block)

