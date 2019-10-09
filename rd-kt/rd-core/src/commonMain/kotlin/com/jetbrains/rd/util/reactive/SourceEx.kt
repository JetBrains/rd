package com.jetbrains.rd.util.reactive

import com.jetbrains.rd.util.Maybe
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.isAlive

/**
 * Adds an event subscription that never gets removed.
 */
fun <T> ISource<T>.adviseEternal(handler: (T) -> Unit) = this.advise(Lifetime.Eternal, handler)

/**
 * Executes [handler] exactly once then terminates the subscription
 */
fun <T> ISource<T>.adviseOnce(lifetime: Lifetime, handler: (T) -> Unit) {
    adviseUntil(lifetime) { handler(it); true }
}

/**
 * Holds subscription until [handler] returns true or [lifetime] is terminated
 */
fun <T> ISource<T>.adviseUntil(lifetime: Lifetime, handler: (T) -> Boolean) {
    if (!lifetime.isAlive) return

    lifetime.createNested { def ->
        this.advise(def) {
            if (handler(it)) def.terminate(true)
        }
    }
}

/**
 * Adds an event subscription that filters out null values.
 */
fun <T : Any> ISource<T?>.adviseNotNull(lifetime: Lifetime, handler: (T) -> Unit) =
        this.advise(lifetime) { if (it != null) handler(it)}

/**
 * Executes [handler] exactly once when the source fires an event with a non-null value,
 * then terminates the subscription
 */
fun <T : Any> ISource<T?>.adviseNotNullOnce(lifetime: Lifetime, handler: (T) -> Unit) = lifetime.createNested { def ->
    this.adviseNotNull(def) {
        def.terminate(true)
        handler(it)
    }
}

fun <T : Any?> ISource<T>.adviseWithPrev(lifetime: Lifetime, handler: (prev: Maybe<T>, cur: T) -> Unit) {
    var prev : Maybe<T> = Maybe.None
    advise(lifetime) { cur ->
        handler(prev, cur)
        prev = Maybe.Just(cur)
    }
}

/**
 * Whenever a change happens in this source, fires a change in the [target] signal obtained
 * by running the given [converter] function.
 */
fun <TSource, TTarget> ISource<TSource>.flowInto(lifetime: Lifetime, target: ISignal<TTarget>, converter: (TSource) -> TTarget) {
    advise(lifetime) { target.fire(converter(it)) }
}

/**
 * Whenever a change happens in this source, fires a change in the [target] signal of the same type.
 */
fun <T> ISource<T>.flowInto(lifetime: Lifetime, target: ISignal<T>) {
    advise(lifetime) { value ->
        if (!target.changing) //forbids recursion
            target.fire(value)
    }
}

/**
 * Whenever a change happens in this source, changes the [target] property of the same type.
 */
fun <T> ISource<T>.flowInto(lifetime: Lifetime, target: IMutablePropertyBase<T>) {
    advise(lifetime) { target.set(it) }
}

fun <T:Any> IViewableSet<T>.flowInto(lifetime: Lifetime, target: IMutableViewableSet<T>) {
    advise(lifetime) { addRemove, v ->
        when (addRemove) {
            AddRemove.Add -> target.add(v)
            AddRemove.Remove -> target.remove(v)
        }
    }
}

fun <T:Any> ISource<IViewableList.Event<T>>.flowInto(lifetime: Lifetime, target: IMutableViewableList<T>) {
    advise(lifetime) { evt ->
        when (evt) {
            is IViewableList.Event.Add -> target.add(evt.index, evt.newValue)
            is IViewableList.Event.Update -> target[evt.index] = evt.newValue
            is IViewableList.Event.Remove -> target.removeAt(evt.index)
        }
    }
}

fun <TKey:Any, TValue: Any> IViewableMap<TKey, TValue>.flowInto(lifetime: Lifetime, target: IMutableViewableMap<TKey, TValue>) {
    advise(lifetime) { evt ->
        when (evt) {
            is IViewableMap.Event.Add -> target[evt.key] = evt.newValue
            is IViewableMap.Event.Update -> target[evt.key] = evt.newValue
            is IViewableMap.Event.Remove -> target.remove(evt.key)
        }
    }
}

/**
 * Whenever a change happens in this source, fires a change in the [target] signal obtained
 * by running the given [converter] function.
 */
fun <TSource, TTarget> ISource<TSource>.flowInto(lifetime: Lifetime, target: IMutablePropertyBase<TTarget>, converter: (TSource) -> TTarget) {
    advise(lifetime) { target.set(converter(it)) }
}

/**
 * Returns a new source which remaps events happening in this source using the given function [f].
 */
fun<T, R> ISource<T>.map(f: (T) -> R) = object : ISource<R> {
    override fun advise(lifetime: Lifetime, handler: (R) -> Unit) {
        this@map.advise(lifetime) { handler(f(it)) }
    }
}


