package com.jetbrains.rider.util.wrappers

import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.lifetime.plusAssign
import com.jetbrains.rider.util.reactive.IProperty
import com.jetbrains.rider.util.reactive.viewNotNull

class Session<T : Any>(val property: IProperty<T?>) {

    var currentLifetime : Lifetime = Lifetime.Eternal
        private set

    init {
        property.viewNotNull (Lifetime.Eternal) { lt, v ->
            currentLifetime = lt
        }
    }

    val currentValue : T? get() = property.value

    fun refresh(value : T) : Lifetime {
        property.value = value
        return currentLifetime
    }

    fun clear() {
        property.value = null
    }
}

fun <T: Any> Session<T>.refreshOnNew(value : T) : Lifetime {
    if (property.value == value) {
        return currentLifetime
    }

    return refresh(value)
}

