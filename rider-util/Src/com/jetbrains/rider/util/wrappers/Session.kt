package com.jetbrains.rider.util.wrappers

import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.lifetime.LifetimeDefinition
import com.jetbrains.rider.util.lifetime.intersect
import com.jetbrains.rider.util.lifetime.plusAssign
import com.jetbrains.rider.util.reactive.IProperty
import com.jetbrains.rider.util.reactive.hasValue
import com.jetbrains.rider.util.reactive.valueOrNull
import com.jetbrains.rider.util.reactive.viewNotNull

class Session<T : Any>(val property : IProperty<T?>) {

    var currentLifetime : Lifetime = Lifetime.Eternal
        private set

    init {
        property.viewNotNull (Lifetime.Eternal) { lt, v ->
            currentLifetime = lt
        }
    }

    val currentValue : T? get() = property.valueOrNull

    fun refresh(value : T, externalLifetime : Lifetime) : Lifetime {
        property.value = value
        externalLifetime += {property.value = null}
        return currentLifetime
    }

    fun clear() {
        property.value = null
    }
}

fun <T: Any> Session<T>.refreshOnNew(value : T, externalLifetime : Lifetime) : Lifetime {
    if (property.hasValue && property.value != null && property.value!!.equals(value)) {
        return currentLifetime
    }

    return refresh(value, externalLifetime)
}

