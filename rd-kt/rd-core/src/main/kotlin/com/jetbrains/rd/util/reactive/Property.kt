package com.jetbrains.rd.util.reactive

import com.jetbrains.rd.util.Sync
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.lifetime.intersect
import com.jetbrains.rd.util.lifetime.isNotAlive
import kotlin.jvm.Volatile

class Property<T>(defaultValue: T) : IProperty<T>
{
    override fun set(newValue: T) {
        value = newValue
    }

    override var value: T = defaultValue
        set(newValue) {
            if (field == newValue) return
            field = newValue
            _change.fire(newValue)
        }

    private val _change = Signal<T>()
    override val change : ISource<T> get() = _change
}

open class OptProperty<T : Any>() : IOptProperty<T> {

    constructor(defaultValue: T) : this() {
        @Suppress("LeakingThis")
        _value = defaultValue
    }

    private val _change = Signal<T>()
    override val change : ISource<T> get() = _change

    override fun set(newValue: T) {
        if (newValue == _value) return
        _value = newValue
        _change.fire(newValue)
    }

    //make it interlocked
    fun setIfEmpty(newValue: T): Boolean {
        if (_value == null) {
            set(newValue)
            return true
        }
        return false
    }

    protected var _value: T? = null

    override val valueOrNull: T?
        get() = _value
}

class WriteOnceProperty<T : Any> : IOptProperty<T> {
    @Volatile
    private var value: T? = null
    private val signal = WriteOnceSignal<T>()
    private val lock = Any()

    override val change: ISource<T> get() = signal
    override val valueOrNull: T? get() = value

    override fun set(newValue: T) {
        if (!setIfEmpty(newValue)) {
            throw IllegalStateException("WriteOnceProperty is already set with `${value}`, but you're trying to rewrite it with `${value}`")
        }
    }

    fun setIfEmpty(newValue: T): Boolean {
        if (value != null)
            return false

        Sync.lock(lock) {
            if (value != null)
                return false

            value = newValue
        }

        signal.fire(newValue)
        return true
    }

    override fun advise(lifetime: Lifetime, handler: (T) -> Unit) {
        if (lifetime.isNotAlive)
            return

        val local = value ?: Sync.lock(lock) {
            if (value == null) // to avoid calling the handler twice
                change.advise(lifetime, handler)

            value
        }

        local?.let { handler(it) }
    }

    override fun view(lifetime: Lifetime, handler: (Lifetime, T) -> Unit) {
        if (lifetime.isNotAlive) return

        advise(lifetime) { v -> handler(lifetime, v) } // value can't be changed and we can pass the outer lifetime
    }

    private class WriteOnceSignal<T> : Signal<T>() {
        private val def = LifetimeDefinition()

        override fun advise(lifetime: Lifetime, handler: (T) -> Unit) {
            if (def.isNotAlive || lifetime.isNotAlive) return

            val nestedDef = def.intersect(lifetime)
            super.advise(nestedDef.lifetime, handler)
        }

        override fun fire(value: T) {
            try {
                super.fire(value)
            } finally {
                def.terminate()
            }
        }
    }

    // for test
    internal fun fireInternal(newValue: T) = signal.fire(newValue)
}

//class Trigger<T : Any>() : OptProperty<T>(), IMutableTrigger<T> {
//    constructor(v: T) : this() {
//        set(v)
//    }
//
//    override fun set(newValue: T) {
//        if (_value != null && _value != newValue) {
//            throw IllegalStateException("Trigger already set with `$_value`, but you try to rewrite it to `$newValue`")
//        }
//
//        super.set(newValue)
//    }

//    override fun wait(cancellationToken: Lifetime, timeoutMs: Long, pump: Pump?): Boolean {
//        //short circuit
//        if (cancellationToken.isTerminated) return false
//        if (hasValue) return true
//
//        val lock = CountDownLatch(1)
//        cancellationToken += {lock.countDown()}
//
//        adviseOnce(cancellationToken) { lock.countDown() }
//        if (cancellationToken.isTerminated) return false
//
//        if (pump == null)
//            return lock.await(timeout.toNanos(), TimeUnit.NANOSECONDS) && hasValue
//
//        val stopTime = System.nanoTime() + timeout.toNanos()
//
//        while (System.nanoTime() < stopTime && !cancellationToken.isTerminated) {
//            pump.pumpAction()
//
//            val awaitTimeNanos = minOf(stopTime - System.nanoTime(), pump.pumpPause.toNanos())
//            if (lock.await(maxOf(awaitTimeNanos, 0), TimeUnit.NANOSECONDS) && hasValue)
//                return true
//        }
//        return false
//    }
//}
