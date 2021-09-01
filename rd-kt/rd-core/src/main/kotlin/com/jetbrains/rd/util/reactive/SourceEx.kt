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
 * by running the given [tf] function.
 */
fun <TSource, TTarget> ISource<TSource>.flowInto(lifetime: Lifetime, target: ISignal<TTarget>, tf: (TSource) -> TTarget) {
    advise(lifetime) {
        if (!target.changing) //forbids recursion
            target.fire(tf(it))
    }
}

/**
 * Whenever a change happens in this source, fires a change in the [target] signal of the same type.
 */
fun <T> ISource<T>.flowInto(lifetime: Lifetime, target: ISignal<T>)  = flowInto(lifetime, target) {it}

/**
 * Whenever a change happens in this source, changes the [target] property of the same type.
 */
fun <TSrc, TDst> ISource<TSrc>.flowInto(lifetime: Lifetime, target: IMutablePropertyBase<TDst>, tf: (TSrc) -> TDst) {
    advise(lifetime) {
        if (target.changing) return@advise

        target.set(tf(it))
    }
}

fun <TSrc> ISource<TSrc>.flowInto(lifetime: Lifetime, target: IMutablePropertyBase<TSrc>) = flowInto(lifetime, target) {it}


fun <TSrc:Any, TDst:Any> ISource<IViewableSet.Event<TSrc>>.flowInto(lifetime: Lifetime, target: IMutableViewableSet<TDst>, tf: (TSrc) -> TDst) {
    advise(lifetime) { (addRemove, v) ->
        if (target.changing) return@advise

        when (addRemove) {
            AddRemove.Add -> target.add(tf(v))
            AddRemove.Remove -> target.remove(tf(v))
        }
    }
}

fun <TSrc:Any, TDst:Any> ISource<IViewableList.Event<TSrc>>.flowInto(lifetime: Lifetime, target: IMutableViewableList<TDst>, tf: (TSrc) -> TDst ) {
    advise(lifetime) { evt ->
        if (target.changing) return@advise

        when (evt) {
            is IViewableList.Event.Add -> target.add(evt.index, tf(evt.newValue))
            is IViewableList.Event.Update -> target[evt.index] = tf(evt.newValue)
            is IViewableList.Event.Remove -> target.removeAt(evt.index)
        }
    }
}

fun <TKey:Any, TValue: Any> ISource<IViewableMap.Event<TKey, TValue>>.flowInto(lifetime: Lifetime, target: IMutableViewableMap<TKey, TValue>, tf: (TValue) -> TValue) {
    advise(lifetime) { evt ->
        if (target.changing) return@advise

        when (evt) {
            is IViewableMap.Event.Add -> target[evt.key] = tf(evt.newValue)
            is IViewableMap.Event.Update -> target[evt.key] = tf(evt.newValue)
            is IViewableMap.Event.Remove -> target.remove(evt.key)
        }
    }
}

/**
 * Returns a new source which remaps events happening in this source using the given function [f].
 */
fun<T, R> ISource<T>.map(f: (T) -> R) = object : ISource<R> {
    override fun advise(lifetime: Lifetime, handler: (R) -> Unit) {
        this@map.advise(lifetime) { handler(f(it)) }
    }
}


