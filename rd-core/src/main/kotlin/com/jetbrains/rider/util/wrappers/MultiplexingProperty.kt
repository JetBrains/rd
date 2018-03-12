package com.jetbrains.rider.util.wrappers

import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.reactive.IProperty
import com.jetbrains.rider.util.reactive.ISource
import com.jetbrains.rider.util.reactive.Signal

class MultiplexingProperty<T, K : Any>(
        private val lifetime: Lifetime,
        private val selector: (T) -> K
) : IProperty<T?> {
    private var _value: T? = null
    private val componentProperties = mutableListOf<Pair<K?, IProperty<T?>>>()
    private val _change = Signal<T?>()
    private var localChange = false

    fun addComponent(property: IProperty<T?>, key: K?) {
        componentProperties.add(0, key to property)
        property.change.advise(lifetime) { newValue ->
            if (!localChange && newValue != value) {
                _value = newValue
                _change.fire(newValue)
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
        _value = newValue
        _change.fire(newValue)
    }

    override var value: T?
        get() = _value
        set(value) {
            set(value)
        }
}
