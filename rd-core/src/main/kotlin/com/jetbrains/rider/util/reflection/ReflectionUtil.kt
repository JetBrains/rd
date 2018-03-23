package com.jetbrains.rider.util.reflection


import com.jetbrains.rider.util.ThreadLocal
import com.jetbrains.rider.util.threadLocalWithInitial
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty


inline fun <T,R> T.usingTrueFlag(flag: KMutableProperty1<T, Boolean>, action: () -> R) : R {
    require(!flag.get(this))

    try {
        flag.set(this, true)
        return action()
    } finally {
        flag.set(this, false)
    }
}

inline fun <T,R> incrementCookie(obj : T, property: KMutableProperty1<T, Int>, action: () -> R) : R {
    try {
        property.set(obj, property.get(obj)+1)
        return action()
    } finally {
        property.set(obj, property.get(obj)-1)

    }
}



fun <T> threadLocal(initialValue: () -> T): ReadWriteProperty<Any?, T> {
    return object : ReadWriteProperty<Any?, T> {
        val storage = threadLocalWithInitial ( initialValue  )
        override fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return storage.get()
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            storage.set(value)
        }

    }
}
