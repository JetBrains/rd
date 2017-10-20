package com.jetbrains.rider.util.reactive

import com.jetbrains.rider.util.Maybe
import com.jetbrains.rider.util.lifetime.Lifetime


open class Property<T>() : IProperty<T> {

    constructor(defaultValue: T) : this() {
        @Suppress("LeakingThis")
        value = defaultValue
    }

    override val change = Signal<T>()
    var name: String?
        get() = change.name
        set(value) {change.name = value}


    override var maybe: Maybe<T> = Maybe.None
        protected set

    override var value : T
        get() = maybe.orElseThrow { IllegalStateException ("Not initialized: $name") }
        set(newValue) {
            maybe.let { if (it is Maybe.Just && newValue == it.value) return}
            maybe = Maybe.Just(newValue)
            change.fire(newValue)
        }

    override fun advise(lifetime: Lifetime, handler: (T) -> Unit) {
        if (lifetime.isTerminated) return

        change.advise(lifetime, handler)
        if (maybe.hasValue) handler(value)
    }

    fun resetValue() {
        maybe = Maybe.None
    }
}

class Trigger<T>() : Property<T>(), IMutableTrigger<T> {
    constructor(v: T) : this() {value = v}

    override var value : T
        get() = super.value
        set(newValue) {
            maybe.let {
                if (it is Maybe.Just)
                    if (value == newValue) return
                    else throw IllegalStateException("Trigger already set with `$value`, but you try to rewrite it to `$newValue`")
            }
            super.value = newValue
        }
}