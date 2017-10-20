package com.jetbrains.rider.util.reactive

import com.jetbrains.rider.util.OfEqualItems
import com.jetbrains.rider.util.binSearch
import com.jetbrains.rider.util.lifetime.Lifetime
import java.util.*

fun <T : Any> IViewableSet<T>.createIsEmpty(lifetime: Lifetime): IReadonlyProperty<Boolean> {
    val property = Property(this.isEmpty())
    this.advise(lifetime) { e -> property.set(this.isEmpty()) }
    return property
}

fun <T : Any> IViewableSet<T>.createIsEmpty() = createIsEmpty(Lifetime.Eternal)
fun <T : Any> IViewableSet<T>.createIsNotEmpty(lifetime: Lifetime) = createIsEmpty(lifetime).not(lifetime)
fun <T : Any> IViewableSet<T>.createIsNotEmpty() = createIsNotEmpty(Lifetime.Eternal)


fun <K : Any, V: Any> IViewableMap<K, V>.createIsEmpty(lifetime: Lifetime): IReadonlyProperty<Boolean> {
    val property = Property(this.isEmpty())
    this.advise(lifetime) { property.set(this.isEmpty()) }
    return property
}

fun <K : Any, V: Any> IViewableMap<K, V>.createIsEmpty() = createIsEmpty(Lifetime.Eternal)
fun <K : Any, V: Any> IViewableMap<K, V>.createIsNotEmpty(lifetime: Lifetime) = createIsEmpty(lifetime).not(lifetime)
fun <K : Any, V: Any> IViewableMap<K, V>.createIsNotEmpty() = createIsNotEmpty(Lifetime.Eternal)

fun <V: Any> IViewableList<V>.flowInto(lifetime: Lifetime, target: IMutableViewableList<V>) {
    flowInto(lifetime, target, { it })
}

// todo replace with map: list.map(x -> b)
fun <TSource: Any, TTarget: Any> IViewableList<TSource>.flowInto(lifetime: Lifetime, target: IMutableViewableList<TTarget>, converter: (TSource) -> TTarget) {
    var guard = false
    advise(lifetime) { kind, value, index ->
        try {
            guard = true
            when (kind) {
                AddRemove.Add -> target.add(index, converter(value))
                AddRemove.Remove -> target.removeAt(index)
            }
        } finally {
            guard = false
        }
    }
    // todo
    // it's bad solution, but for now I don't know how to allow modifications into flowInto subscription and
    // disallow it outside at the same time except any cookie
    try {
        guard = true
        target.advise(lifetime) { evt ->
            if (guard) return@advise
            throw IllegalStateException("Attempt to modify flowInto's target (which is readonly by design).")
        }
    } finally {
        guard = false
    }
}

// todo replace with orderBy: list.orderBy(x -> b)
fun <T : Any> IViewableList<T>.flowIntoSorted(lifetime: Lifetime, target: IMutableViewableList<T>, comparator: Comparator<T>,
                                              filter: (T) -> Boolean) {
    lifetime.bracket({ target.clear() }, { target.clear() })

    this.advise(lifetime) { kind, item, index ->
        if (filter(item).not()) return@advise

        val idx = target.binSearch(item, comparator, which = OfEqualItems.TakeLast)
        when (kind) {
            AddRemove.Add -> {
                val insertionPosition = if (idx >= 0) idx + 1 else -idx - 1
                target.add(insertionPosition, item)
            }
            AddRemove.Remove -> {
                if (idx < 0) throw IllegalStateException("All source collection elements must be presented into target collection.")
                target.removeAt(idx)
            }
        }
    }
}