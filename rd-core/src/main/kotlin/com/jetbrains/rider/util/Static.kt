package com.jetbrains.rider.util

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

object Static {
    private val map: ConcurrentHashMap<KClass<*>, List<Any>> = ConcurrentHashMap()

    fun <T:Any> push(kclass: KClass<T>,  value: T) {
        map.merge(kclass, listOf(value)) {
            oldList, wrappedValue -> oldList + wrappedValue
        }
    }

    fun <T:Any> pop(kclass: KClass<T>) : T? {
        val lst = map.merge(kclass, emptyList()) { oldList, _ ->
            val newList = oldList.dropLast(1)
            if (newList.isEmpty())
                null
            else
                newList
        }
        @Suppress("UNCHECKED_CAST")
        return lst?.lastOrNull() as T?
    }

    fun <T:Any> peek(kclass: KClass<T>) : T? {
        @Suppress("UNCHECKED_CAST")
        return map[kclass]?.lastOrNull() as T?
    }
}

//Please don't specify such a helper for [push] because it's easy to obtain child class instead of parent
inline fun <reified T : Any> Static.pop() : T? = pop(T::class)
inline fun<T:Any, R: Any?> Static.use(klass: KClass<T>, value: T, action: () -> R) :R {
    try {
        push(klass, value)
        return action()
    } finally {
        pop(klass)
    }
}

