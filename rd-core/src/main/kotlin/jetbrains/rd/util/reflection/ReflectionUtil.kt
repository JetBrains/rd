package com.jetbrains.rider.util.reflection

import kotlin.concurrent.thread
import kotlin.properties.Delegates
import kotlin.properties.ObservableProperty
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

class X {
    var p : Int by threadLocal(0)
}

fun <T> threadLocal(initialValue: T): ReadWriteProperty<Any?, T> {
    return object : ReadWriteProperty<Any?, T> {
        val storage = ThreadLocal.withInitial { initialValue  }
        override fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return storage.get()
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            storage.set(value)
        }

    }
}

fun main(args: Array<String>) {

    val x = X()
    for (i in 1..3) {
        thread {
            println("${Thread.currentThread().id}: ${x.p}")

            incrementCookie(x, X::p) {
                println("${Thread.currentThread().id}: ${x.p}")

                incrementCookie(x, X::p) {
                    println("${Thread.currentThread().id}: ${x.p}")
                }
            }

            println("${Thread.currentThread().id}: ${x.p}")
        }
    }
}
