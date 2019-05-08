package com.jetbrains.rd.util.wrappers

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.intersect
import com.jetbrains.rd.util.lifetime.plusAssign
import com.jetbrains.rd.util.reactive.IProperty
import com.jetbrains.rd.util.reactive.ISource
import com.jetbrains.rd.util.reactive.Signal

class MultiplexingProperty<T, K : Any>(
        private val lifetime: Lifetime,
        private val selector: (T) -> K
) : IProperty<T?> {
    private var _value: T? = null
    private val componentProperties = mutableListOf<Pair<K?, IProperty<T?>>>()
    private val _change = Signal<T?>()
    private var localChange = false

    fun addComponent(property: IProperty<T?>, key: K?) {
        doAddComponent(property, key, lifetime)
    }

    private fun doAddComponent(property: IProperty<T?>, key: K?, subscriptionLifetime: Lifetime) {
        componentProperties.add(0, key to property)
        property.change.advise(subscriptionLifetime) { newValue ->
            if (!localChange && newValue != value) {
                setThisValue(newValue)
            }
        }
        if (componentProperties.size == 1 && property.value != null) {
            setThisValue(property.value)
        }
    }

    fun addComponent(property: IProperty<T?>, key: K?, componentLifetime: Lifetime) {
        doAddComponent(property, key, componentLifetime.intersect(lifetime))
        componentLifetime += {
            componentProperties.remove(key to property)
            val newValue = componentProperties.firstOrNull()?.second?.value
            if (newValue != _value) {
                setThisValue(newValue)
            }
        }
    }

    override val change: ISource<T?>
        get() = _change

    override fun set(newValue: T?) {
        val newKey = newValue?.let { selector(it) }
        localChange = true
        try {
            for ((key, componentProperty) in componentProperties) {
                componentProperty.set(if (key == newKey) newValue else null)
            }
        }
        finally {
            localChange = false
        }
        setThisValue(newValue)
    }

    private fun setThisValue(newValue: T?) {
        _value = newValue
        _change.fire(newValue)
    }

    override var value: T?
        get() = _value
        set(value) {
            set(value)
        }
}
