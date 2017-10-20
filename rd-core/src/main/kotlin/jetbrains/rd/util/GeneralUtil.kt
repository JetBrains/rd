package com.jetbrains.rider.util

import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.lifetime.ifAlive
import com.jetbrains.rider.util.lifetime.plusAssign
import com.jetbrains.rider.util.reactive.*
import org.apache.commons.logging.LogFactory
import java.util.ArrayList
import java.util.HashMap
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KMutableProperty1

inline fun using(disposable : IDisposable, block: () -> Unit) {
    try {
        return block()
    } finally {
        disposable.dispose()
    }
}

fun String.toNullIfEmpty() : String? {
    return if(isEmpty()) null else this
}

fun String?.getPlatformIndependentHash() : Int = this?.fold(19, {acc, c -> acc*31 + c.toInt()}) ?:0

fun equals(a: Any?, b: Any?):Boolean  {
    if (a === b) return true
    if (a == null || b == null) return false
    return (a.equals(b))
}

inline fun <reified T>  T.iff(condition : Boolean, transform : (T) -> T) : T {
    return if (condition) transform(this) else this
}

inline fun <reified T>  T.iff(condition : (T) -> Boolean, transform : (T) -> T) : T {
    return if (condition(this)) transform(this) else this
}

inline fun Boolean.onTrue (f : () -> Unit) : Unit {
    if (this) f()
}
inline fun Boolean.onFalse (f : () -> Unit) : Unit {
    if (!this) f()
}

inline fun Boolean.condstr(f: () -> String) : String {
    return if (this) f() else ""
}

fun String.optSpace(space : String = " ") = iff (this.isNotEmpty()) { it + space }

fun <T> Iterable<T>.joinToOptString(separator: CharSequence = ", ", prefix: CharSequence = "", postfix: CharSequence = "", transform: ((T) -> CharSequence)? = null): String {
    if (this.iterator().hasNext())
        return joinToString(separator, prefix, postfix, -1, "...", transform)
    else
        return ""
}

fun <T> Array<T>.joinToOptString(separator: CharSequence = ", ", prefix: CharSequence = "", postfix: CharSequence = "", transform: ((T) -> CharSequence)? = null): String {
    return toList().joinToOptString(separator, prefix, postfix, transform)
}
