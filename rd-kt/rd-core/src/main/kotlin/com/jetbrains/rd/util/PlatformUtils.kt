package com.jetbrains.rd.util

import com.jetbrains.rd.util.threading.SpinWait
import java.io.Closeable
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.io.use
import kotlin.reflect.KClass

fun currentThreadName() : String = Thread.currentThread().run { "$id:$name"}

fun <T> threadLocalWithInitial(initial: () -> T) : ThreadLocal<T> = ThreadLocal.withInitial(initial)

val eol : String = System.lineSeparator()

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
object Sync {
    @OptIn(ExperimentalContracts::class)
    inline fun <R: Any?> lock(obj: Any, acton: () -> R): R {
        contract {
            callsInPlace(acton, InvocationKind.EXACTLY_ONCE)
        }

        return synchronized(obj, acton)
    }
    fun notifyAll(obj: Any) = (obj as Object).notifyAll()
    fun wait(obj: Any) = (obj as Object).wait()
    fun wait(obj: Any, timeout: Long) = (obj as Object).wait(timeout)
}

// For the protocol generator to avoid imports from java.util:
typealias Date = java.util.Date
typealias EnumSet<T> = java.util.EnumSet<T>

inline fun <T : Closeable?, R> T.use(block:(T) -> R) : R = use(block)

fun Throwable.getThrowableText(): String = StringWriter().apply { printStackTrace(PrintWriter(this)) }.toString()

fun qualifiedName(kclass: KClass<*>) : String = kclass.qualifiedName?:"<anonymous>"

fun measureTimeMillis(block: () -> Unit): Long = kotlin.system.measureTimeMillis(block)

fun printlnError(msg: String) = System.err.println(msg)

fun assert(value: Boolean) = kotlin.assert(value)

inline fun assert(value: Boolean, lazyMessage: () -> Any)  = kotlin.assert(value, lazyMessage)

inline fun spinUntil(condition: () -> Boolean) = SpinWait.spinUntil(condition)
inline fun spinUntil(timeoutMs: Long, condition: () -> Boolean) = SpinWait.spinUntil(timeoutMs, condition)

inline fun <reified T : Enum<T>> enumSetOf(values: Set<T> = emptySet()) : EnumSet<T> = EnumSet.noneOf(T::class.java).apply { addAll(values) }
fun <T: Enum<T>> EnumSet<T>.values() : Set<T> = this
