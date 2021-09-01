package com.jetbrains.rd.util.reactive

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.onTermination

fun <T : Any> IViewableSet<T>.createIsEmpty(lifetime: Lifetime): IPropertyView<Boolean> {
    val property = Property(this.isEmpty())
    this.advise(lifetime) { _ -> property.set(this.isEmpty()) }
    return property
}

fun <T : Any> IViewableSet<T>.createIsEmpty() = createIsEmpty(Lifetime.Eternal)
fun <T : Any> IViewableSet<T>.createIsNotEmpty(lifetime: Lifetime) = createIsEmpty(lifetime).not()
fun <T : Any> IViewableSet<T>.createIsNotEmpty() = createIsNotEmpty(Lifetime.Eternal)

fun <K : Any, V: Any> IViewableMap<K, V>.createIsEmpty(lifetime: Lifetime): IPropertyView<Boolean> {
    val property = Property(this.isEmpty())
    this.advise(lifetime) { property.set(this.isEmpty()) }
    return property
}

fun <K : Any, V : Any> IViewableMap<K, V>.createIsEmpty() = createIsEmpty(Lifetime.Eternal)
fun <K : Any, V : Any> IViewableMap<K, V>.createIsNotEmpty(lifetime: Lifetime) = createIsEmpty(lifetime).not()
fun <K : Any, V : Any> IViewableMap<K, V>.createIsNotEmpty() = createIsNotEmpty(Lifetime.Eternal)

/**
 * Converts a viewable map to a viewable list of values, where the [converter] function is used to
 * convert each map value to a list value.
 */
fun <K : Any, V : Any, T : Any> IMutableViewableMap<K, V>.toViewableList(lifetime: Lifetime, converter: (Lifetime, V) -> T): ViewableList<T> {
    val viewableList = ViewableList<T>()
    this.view(lifetime) { lt, (_, item) ->
        val t = converter(lt, item)
        viewableList.add(t)
        lt.onTermination { viewableList.remove(t) }
    }
    return viewableList
}

/**
 * Returns a property which evaluates to the last element of the list and is updated as elements
 * are added to the list or removed from it.
 */
fun <T : Any> IViewableList<T>.viewableTail() : IPropertyView<T?> = object : IPropertyView<T?> {
    override val change: ISource<T?> = object : ISource<T?> {
        override fun advise(lifetime: Lifetime, handler: (T?) -> Unit) {
            this@viewableTail.change.advise(lifetime) { evt ->
                val offset = if (evt is IViewableList.Event.Remove) 0 else -1
                if (evt.index == size + offset)
                    handler(value)
            }
        }
    }

    override val value: T?
        get() = lastOrNull()
}
