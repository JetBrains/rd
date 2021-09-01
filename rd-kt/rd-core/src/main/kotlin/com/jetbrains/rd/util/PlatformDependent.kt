package com.jetbrains.rd.util

import com.jetbrains.rd.util.threading.SpinWait
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Date
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.use
import kotlin.reflect.KClass

typealias ExecutionException = ExecutionException

fun currentThreadName() : String = Thread.currentThread().run { "$id:$name"}

class AtomicReference<T> constructor(initial: T) {
    private val impl = AtomicReference(initial)
    fun get(): T = impl.get()
    fun getAndUpdate(f: (T) -> T): T = impl.getAndUpdate(f)
    fun getAndSet(newValue: T): T = impl.getAndSet(newValue)
    fun compareAndSet(expectedValue: T, newValue: T): Boolean = impl.compareAndSet(expectedValue, newValue)
}

typealias CancellationException = CancellationException
typealias TimeoutException = TimeoutException

typealias ThreadLocal<T> = java.lang.ThreadLocal<T>

fun <T> threadLocalWithInitial(initial: () -> T) : ThreadLocal<T> = ThreadLocal.withInitial(initial)

val eol : String = System.lineSeparator()

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
object Sync {
    inline fun <R: Any?> lock(obj: Any, acton: () -> R) = synchronized(obj, acton)
    fun notifyAll(obj: Any) = (obj as Object).notifyAll()
    fun notify(obj: Any) = (obj as Object).notify()
    fun wait(obj: Any) = (obj as Object).wait()
    fun wait(obj: Any, timeout: Long) = (obj as Object).wait(timeout)
}

fun<K,V> concurrentMapOf() : MutableMap<K,V> = ConcurrentHashMap()

typealias Closeable = java.io.Closeable
inline fun <T : Closeable?, R> T.use(block:(T) -> R) : R = use(block)

fun Throwable.getThrowableText(): String = StringWriter().apply { printStackTrace(PrintWriter(this)) }.toString()

fun qualifiedName(kclass: KClass<*>) : String = kclass.qualifiedName?:"<anonymous>"

fun measureTimeMillis(block: () -> Unit): Long = kotlin.system.measureTimeMillis(block)

//special jvm classes
typealias URI = java.net.URI

typealias Date = Date

typealias UUID = java.util.UUID

typealias AtomicInteger = AtomicInteger

typealias Queue<E> = java.util.concurrent.LinkedBlockingQueue<E>
typealias ConcurrentHashMap<K, V> = java.util.concurrent.ConcurrentHashMap<K, V>

fun printlnError(msg: String) = System.err.println(msg)

fun assert(value: Boolean) = kotlin.assert(value)

inline fun assert(value: Boolean, lazyMessage: () -> Any)  = kotlin.assert(value, lazyMessage)

inline fun spinUntil(condition: () -> Boolean) = SpinWait.spinUntil(condition)
inline fun spinUntil(timeoutMs: Long, condition: () -> Boolean) = SpinWait.spinUntil(timeoutMs, condition)

typealias EnumSet<T> = java.util.EnumSet<T>
inline fun <reified T : Enum<T>> enumSetOf(values: Set<T> = emptySet()) : EnumSet<T> = EnumSet.noneOf(T::class.java).apply { addAll(values) }
fun <T: Enum<T>> EnumSet<T>.values() : Set<T> = this

typealias Runnable = java.lang.Runnable
typealias Callable<T> = java.util.concurrent.Callable<T>

typealias CopyOnWriteArrayList<T> = java.util.concurrent.CopyOnWriteArrayList<T>