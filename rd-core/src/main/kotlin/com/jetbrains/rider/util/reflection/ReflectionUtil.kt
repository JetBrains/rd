package com.jetbrains.rider.util.reflection


import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty


inline fun <T,R> T.usingTrueFlag(flag: KMutableProperty1<T, Boolean>, action: () -> R) : R {
    assert(!flag.get(this))

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
        val storage = ThreadLocal.withInitial ( initialValue  )
        override fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return storage.get()
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            storage.set(value)
        }

    }
}
