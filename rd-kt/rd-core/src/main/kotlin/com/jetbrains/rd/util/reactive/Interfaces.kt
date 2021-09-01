package com.jetbrains.rd.util.reactive

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.SequentialLifetimes
import com.jetbrains.rd.util.lifetime.isAlive
import com.jetbrains.rd.util.lifetime.onTermination


/**
 * An object that allows to subscribe to events of type [T].
 */
interface ISource<out T> {
    /**
     * Adds an event subscription. Every time an event occurs, the [handler] is called, receiving
     * an instance of the event. The subscription is removed when the given [lifetime] expires.
     */
    fun advise(lifetime: Lifetime, handler: (T) -> Unit)
}

/**
 * An object that allows to subscribe to events of type [T] and to handle them on a different thread.
 */
interface IAsyncSource<out T> : ISource<T> {
    /**
     * Adds an event subscription. Every time an event occurs, the [handler] is called on the given [scheduler],
     * receiving an instance of the event. The subscription is removed when the given [lifetime] expires.
     */
    fun adviseOn(lifetime : Lifetime, scheduler : IScheduler, handler : (T) -> Unit)
}

typealias IVoidSource = ISource<Unit>
fun IVoidSource.advise(lifetime: Lifetime, handler: () -> Unit) = advise(lifetime) { handler() }
fun IAsyncSource<Unit>.adviseOn(lifetime : Lifetime, scheduler : IScheduler, handler : () -> Unit) = adviseOn(lifetime, scheduler) { handler() }

/**
 * An object that allows to subscribe to changes of its contents.
 */
interface IViewable<out T> {
    /**
     * Adds a subscription to changes of the contents of the object.
     *
     * Every time the contents changes (e.g. the value of a property changes, or an object is added to a collection),
     * the [handler] is called receiving the new contents. The [Lifetime] instance passed to the handler expires when
     * the value is no longer part of the contents (e.g. the value of a property changes to something else, or the object is
     * removed from the collection).
     *
     * The subscription is removed when the given [lifetime] expires.
     */
    fun view(lifetime: Lifetime, handler: (Lifetime, T) -> Unit)
}

/**
 * Adds a subscription to changes to this viewable's contents and filters out null values.
 * The subscription is removed when the given [lifetime] expires.
 */
fun <T : Any> IViewable<T?>.viewNotNull(lifetime: Lifetime, handler: (Lifetime, T) -> Unit) =
        this.view(lifetime) { lf, v -> if (v != null) handler(lf, v) }

/**
 * Common base class of optional and non-optional properties. Shouldn't be used in client code.
 */
interface IPropertyBase<out T> : ISource<T>, IViewable<T> {
    /**
     * Allows to subscribe to subsequent changes of the property value. Unlike subscribing to the property directly,
     * the callback will not be immediately called with the current value of the property.
     */
    val change: ISource<T>

    override fun view(lifetime: Lifetime, handler: (Lifetime, T) -> Unit) {
        if (!lifetime.isAlive) return

        // nested lifetime is needed due to exception that could be thrown
        // while viewing a property change right at the moment of <param>lifetime</param>'s termination
        // but before <param>handler</param> gets removed (e.g. p.view(lf) { /*here*/ }; lf += { p.set(..) })
        val lf = lifetime.createNested()
        SequentialLifetimes(lf).let {
            advise(lf) { v ->
                if (lf.isAlive) handler(it.next(), v)
            }
        }
    }
}

/**
 * A read-only property that can be observed.
 */
interface IPropertyView<out T> : IPropertyBase<T> {
    /**
     * Returns the current value of the property.
     */
    val value: T

    operator fun invoke() = value

    override fun advise(lifetime: Lifetime, handler: (T) -> Unit) {
        if (!lifetime.isAlive)
            return

        change.advise(lifetime, handler)
        handler(value)
    }
}

/**
 * A read-only property that is created in an uninitialized state and can be observed.
 */
interface IOptPropertyView<out T : Any> : IPropertyBase<T> {
    /**
     * Returns the current value of the property, or null if the property has not been initialized.
     */
    val valueOrNull: T?

    override fun advise(lifetime: Lifetime, handler: (T) -> Unit) {
        if (!lifetime.isAlive)
            return

        change.advise(lifetime, handler)
        valueOrNull?.let { handler(it) }
    }
}

/**
 * Evaluates to true if the property has an initialized value.
 */
val <T : Any> IOptPropertyView<T>.hasValue : Boolean get() = valueOrNull != null

/**
 * Returns the value of the property, or throws an exception if it was not initialized.
 */
val <T : Any> IOptPropertyView<T>.valueOrThrow: T
    get() = valueOrNull ?: throw IllegalStateException("Property has not been initialized")

/**
 * Returns the value of the property, or the given default value if the property was not initialized.
 */
fun <T : Any> IOptProperty<T>.valueOrDefault(default: T) : T = valueOrNull ?: default


/**
 * Converts an optional property to a non-optional property with a nullable value type.
 */
fun <T : Any> IOptPropertyView<T>.asNullable(): IPropertyView<T?> = object : IPropertyView<T?> {
    override val value: T? get() = valueOrNull
    override val change: ISource<T?> get() = this@asNullable.change
}

/**
 * An object which has a collection of event listeners and can broadcast an event to the listeners.
 */
interface ISignal<T> : ISource<T> {
    val changing : Boolean
    fun fire(value : T)
}

interface IAsyncSignal<T> : ISignal<T>, IAsyncSource<T>  {
    var scheduler: IScheduler
}

typealias IVoidSignal = ISignal<Unit>
typealias IAsyncVoidSignal = IAsyncSignal<Unit>

//Touching the Void
fun IVoidSignal.fire() = fire(Unit)

interface IMutablePropertyBase<T> : IPropertyBase<T> {
    val changing: Boolean get() = (change as? ISignal<T>)?.changing ?: false
    /**
     * Sets the property to the given value.
     */
    fun set(newValue: T)
}

/**
 * A mutable property.
 */
interface IProperty<T> : IPropertyView<T>, IMutablePropertyBase<T> {
    override var value : T
}

/**
 * Sets the property value to the given [value] for the duration of the [lifetime],
 * and resets it to null when the lifetime is terminated.
 */
fun <T> IProperty<T?>.setValue(lifetime: Lifetime, value: T?) {
    this.value = value
    lifetime.onTermination {
        this.value = null
    }
} //for backward compatibility

/**
 * A mutable property that is created in an uninitialized state.
 */
interface IOptProperty<T : Any> : IOptPropertyView<T>, IMutablePropertyBase<T>

class Pump(val pumpAction: (() -> Unit), val pumpPauseMs: Long)

//interface ITrigger<T : Any> : IOptPropertyView<T> {
//    fun wait(cancellationToken : Lifetime, timeout: Long, pump: Pump? = null) : Boolean
//}

//fun<T : Any> ITrigger<T>.wait() : Boolean = wait(Lifetime.Eternal, InfiniteDuration)
//fun<T : Any> ITrigger<T>.wait(timeout: Long) : Boolean = wait(Lifetime.Eternal, timeout)
//fun <T : Any> ITrigger<T>.waitAndPump(pumpAction: () -> Unit): Boolean = wait(Lifetime.Eternal, InfiniteDuration, Pump(pumpAction, 50))

//interface IMutableTrigger<T : Any> : IOptProperty<T>, ITrigger<T>
