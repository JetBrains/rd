package com.jetbrains.rider.util

import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.lifetime.ifAlive
import com.jetbrains.rider.util.lifetime.plusAssign

fun <T> MutableCollection<T>.addIfNotExists(value : T) : Boolean {
    if (contains(value)) return false
    add(value)
    return true
}


fun <T> Iterable<T>.join(separator : String) : String {
    any().onFalse { return "<null>" }
    return StringBuilder().apply {
        append(first())
        drop(1).forEach {
            append(separator)
            append(it)
        }
    }.toString()
}

fun <T> MutableCollection<T>.addUnique(lifetime : Lifetime, value : T) {
    lifetime.ifAlive {
        add(value).onFalse { throw IllegalArgumentException("Value already exists: $value") }
        lifetime += { this.remove(value) }
    }
}

fun <K,V> MutableMap<K, V>.addUnique(lifetime : Lifetime, key: K, value : V) : V {
    lifetime.ifAlive {
        put(key, value)?.let { throw IllegalArgumentException("Value already exists: $value") }
        lifetime += { this.remove(key) }
    }
    return value
}

fun <T> MutableCollection<T>.blockingAddUnique(lifetime : Lifetime, value : T, lock: Any = this) {
    lifetime.ifAlive {
        synchronized(lock) {
            add(value).onFalse { throw IllegalArgumentException("Value already exists: $value") }
            lifetime += { synchronized(lock) {remove(value)} }
        }
    }
}

fun <K, V> MutableMap<K, V>.putUnique(key: K, value: V) : V {
    this[key]?. let {throw IllegalStateException("Value already exist for $key") }
    this[key] = value
    return value
}

fun <K, V> MutableMap<K, V>.putUnique(lf: Lifetime, key: K, value: V) {
    this[key]?. let {throw IllegalStateException("Value already exist for $key") }
    this.put(lf, key, value)
}

fun <K, V> MutableMap<K, V>.put(lf: Lifetime, key: K, value: V) {
    lf.bracket({this[key] = value}, {
        this.remove(key)
    })
}

fun <K, V> MutableMap<K, V>.blockingPutUnique(lf: Lifetime, lock: Any, key: K, value: V) {
    lf.ifAlive {
        synchronized(lock) {
            this[key]?. let {throw IllegalStateException("Value $it already exist for key $key") }
            this[key] = value
            lf += {
                synchronized(lock) {
                    this.remove(key) ?: let { throw IllegalStateException("No value removed for key $key") }
                }
            }
        }
    }
}


fun <K,V> MutableMap<K,V>.put(entry: Map.Entry<K,V>) = put(entry.key, entry.value)

fun <TKey, TValue> Map<TKey,TValue>.first(): Map.Entry<TKey, TValue> {
    for (v in this) {
        return v
    }
    throw NoSuchElementException("Map is empty.")
}

fun <TKey, TValue> Map<TKey,TValue>.firstOrNull(): Map.Entry<TKey, TValue>? {
    for (v in this) {
        return v
    }
    return null
}

inline fun <K : Any, V : Any> MutableMap<K, V>.getOrCreate(key : K, crossinline creator :(K) -> V) : V {
    return this[key] ?: creator(key).let {
        this[key] = it
        return it
    }
}

inline fun <reified T> Array<T>.insert(elt: T, idx: Int) : Array<T> {
    return Array(size+1) { i ->
        if (i < idx) this[i]
        else if (i > idx) this[i-1]
        else elt
    }
}

inline fun <reified T> Array<T>.remove(elt: T) : Array<T> {
    val idx = this.indexOf(elt)
    if (idx == -1) return this

    return Array(size-1) { i ->
        if (i < idx) this[i]
        else this[i+1]
    }
}

inline fun <T> Array<out T>.forEachReversed(action: (T) -> Unit) {
    var i = size
    while (i > 0) {
        action(this[--i])
    }
}

fun <T> List<T>?.restOrNull(): List<T>? = if (this == null || this.isEmpty() || this.size == 1) null else this.subList(1, size)