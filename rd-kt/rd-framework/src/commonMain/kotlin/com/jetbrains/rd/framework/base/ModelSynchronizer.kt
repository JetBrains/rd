package com.jetbrains.rd.framework.base

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.*


fun<T> synchronize(lifetime: Lifetime, a: ISignal<T>, b: ISignal<T>) {
    a.flowInto(lifetime, b)
    b.flowInto(lifetime, a)
}

fun<T> synchronize(lifetime: Lifetime, a: IMutablePropertyBase<T>, b: IMutablePropertyBase<T>) {
    a.flowInto(lifetime, b)
    b.flowInto(lifetime, a)
}

fun<T: Any> synchronize(lifetime: Lifetime, a: IMutableViewableSet<T>, b: IMutableViewableSet<T>) {
    a.flowInto(lifetime, b)
    b.flowInto(lifetime, a)
}

fun<K: Any, V: Any> synchronize(lifetime: Lifetime, a: IMutableViewableMap<K, V>, b: IMutableViewableMap<K, V>) {
    a.flowInto(lifetime, b)
    b.flowInto(lifetime, a)
}

fun<T: Any> synchronize(lifetime: Lifetime, a: IMutableViewableList<T>, b: IMutableViewableList<T>) {
    require(a.size == b.size) { "List initial states must be synchronized, but sizes are different: ${a.size} != ${b.size}" }
    a.forEachIndexed { index, v ->
        require(v == b[index]) { "List initial states must be synchronized, but elements are different at index $index: ${v} != ${b[index]}" }
    }

    a.flowInto(lifetime, b)
    b.flowInto(lifetime, a)
}


@Suppress("UNCHECKED_CAST")
internal fun synchronizePolymorphic(lifetime: Lifetime, first: Any?, second: Any?) {

    if (first is ISignal<*> && second is ISignal<*>) {
        synchronize(lifetime, first as ISignal<Any>, second as ISignal<Any>)

    } else if (first is IMutablePropertyBase<*> && second is IMutablePropertyBase<*>) {
        synchronize(lifetime, first as IMutablePropertyBase<Any>, second as IMutablePropertyBase<Any>)

    } else if (first is IMutableViewableList<*> && second is IMutableViewableList<*>) {
        synchronize(lifetime, first as IMutablePropertyBase<Any>, second as IMutablePropertyBase<Any>)

    } else if (first is IMutableViewableMap<*, *> && second is IMutableViewableMap<*, *>) {
        synchronize(lifetime, first as IMutableViewableMap<Any, Any>, second as IMutableViewableMap<Any, Any>)

    } else if (first is RdBindableBase && second is RdBindableBase) {
        first.synchronizeWith(lifetime, second)
    }
}