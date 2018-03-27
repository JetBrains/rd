package com.jetbrains.rider.util.reactive

import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.lifetime.LifetimeDefinition
import com.jetbrains.rider.util.putUnique

/**
 * A type of change in a collection.
 */
enum class AddRemove { Add, Remove }

/**
 * A set allowing its contents to be observed.
 */
interface IViewableSet<T : Any> : Set<T>, IViewable<T>, ISource<IViewableSet.Event<T>> {
    /**
     * Represents an addition or removal of an element in the set.
     */
    data class Event<T>(val kind: AddRemove, val value: T)

    /**
     * Adds a subscription for additions and removals of set elements. When the subscription is initially
     * added, [handler] is called with [AddRemove.Add] events for all elements currently in the set.
     */
    fun advise(lifetime: Lifetime, handler: (AddRemove, T) -> Unit) =
            advise(lifetime) { evt -> handler(evt.kind, evt.value) }

    /**
     * Adds a subscription to changes of the contents of the set.
     *
     * When [handler] is initially added, it is called receiving all elements currently in the set.
     * Every time an object is added to the set, the [handler] is called receiving the new element.
     * The [Lifetime] instance passed to the handler expires when the element is removed from the set.
     *
     * The subscription is removed when the given [lifetime] expires.
     */
    override fun view(lifetime: Lifetime, handler: (Lifetime, T) -> Unit) {
        val lifetimes = hashMapOf<T, LifetimeDefinition>()
        advise(lifetime) { kind, v ->
            when (kind) {
                AddRemove.Add -> {
                    val def = lifetimes.putUnique(v, Lifetime.create(lifetime))
                    handler(def.lifetime, v)
                }
                AddRemove.Remove -> lifetimes.remove(v)!!.terminate()
            }
        }
    }
}

interface IMutableViewableSet<T : Any> : MutableSet<T>, IViewableSet<T>


data class KeyValuePair<K, V>(override val key: K, override val value: V) : Map.Entry<K, V>

/**
 * A map allowing its contents to be observed.
 */
interface IViewableMap<K : Any, V:Any> : Map<K, V>, IViewable<Map.Entry<K, V>>, ISource<IViewableMap.Event<K, V>> {
    /**
     * Represents an addition, update or removal of an element in the map.
     */
    sealed class Event<K, V>(val key: K) {
        class Add<K,V>   (key: K,                   val newValue : V) : Event<K,V>(key) {
            override fun toString() = "Add $key:$newValue"
        }
        class Update<K,V>(key: K, val oldValue : V, val newValue : V) : Event<K,V>(key) {
            override fun toString() = "Update $key:$newValue"
        }
        class Remove<K,V>(key: K, val oldValue : V                  ) : Event<K,V>(key) {
            override fun toString() = "Remove $key"
        }

        /**
         * Returns the new value for add or update events, or null for removal events.
         */
        val newValueOpt: V? get() = when (this) {
            is Event.Add    -> this.newValue
            is Event.Update -> this.newValue
            else -> null
        }
    }

    /**
     * Adds a subscription to changes of the contents of the map.
     *
     * When [handler] is initially added, it is called receiving all key/value pairs currently in the map.
     * Every time a key/value pair is added to the map, the [handler] is called receiving the new key/value pair.
     * The [Lifetime] instance passed to the handler expires when the key/value pair is removed from the map.
     *
     * The subscription is removed when the given [lifetime] expires.
     */
    override fun view(lifetime: Lifetime, handler: (Lifetime, Map.Entry<K, V>) -> Unit) {
        val lifetimes = hashMapOf<K, LifetimeDefinition>()
        adviseAddRemove(lifetime) { kind, key, value ->
            val entry = KeyValuePair(key, value)
            when (kind) {
                AddRemove.Add -> {
                    val def = lifetimes.putUnique(key, Lifetime.create(lifetime))
                    handler(def.lifetime, entry)
                }
                AddRemove.Remove -> {
                    val remove = lifetimes.remove(key) ?: error("attempting to remove non-existing item $entry")
                    remove.terminate()
                }
            }
        }
    }

    /**
     * Adds a subscription to additions and removals of map elements. When a map element is updated, the [handler]
     * is called twice: to report the removal of the old element and the addition of the new one.
     *
     * The subscription is removed when the given [lifetime] expires.
     */
    fun adviseAddRemove(lifetime: Lifetime, handler: (AddRemove, K, V) -> Unit) {
        advise(lifetime) { when (it) {
            is Event.Add -> handler(AddRemove.Add, it.key, it.newValue)
            is Event.Update -> {
                handler(AddRemove.Remove, it.key, it.oldValue)
                handler(AddRemove.Add, it.key, it.newValue)
            }
            is Event.Remove -> handler(AddRemove.Remove, it.key, it.oldValue)
        }}
    }

    /**
     * Adds a subscription to changes of the contents of the map, with the handler receiving keys and values as separate
     * parameters.
     *
     * When [handler] is initially added, it is called receiving all keys and values currently in the map.
     * Every time a key/value pair is added to the map, the [handler] is called receiving the new key and value.
     * The [Lifetime] instance passed to the handler expires when the key/value pair is removed from the map.
     *
     * The subscription is removed when the given [lifetime] expires.
     */
    fun view(lifetime: Lifetime, handler: (Lifetime, K, V) -> Unit) =
            view(lifetime, { lf, entry -> handler(lf, entry.key, entry.value) })
}

/**
 * A list allowing its contents to be observed.
 */
interface IViewableList<out V: Any> : List<V>, IViewable<Pair<Int, V>>, ISource<IViewableList.Event<V>> {

    val change: ISource<IViewableList.Event<V>>
    /**
     * Represents an addition, update or removal of an element in the list.
     */
    sealed class Event<out V>(val index: Int) {
        class Add<V>   (index: Int, val newValue : V) : Event<V>(index)
        class Update<V>(index: Int, val oldValue : V, val newValue : V) : Event<V>(index)
        class Remove<V>(index: Int, val oldValue : V                  ) : Event<V>(index)

        /**
         * Returns the new value for add or update events, or null for removal events.
         */
        val newValueOpt: V? get() = when (this) {
            is Event.Add    -> this.newValue
            is Event.Update -> this.newValue
            else -> null
        }
    }

    /**
     * Adds a subscription to additions and removals of list elements. When a list element is updated, the [handler]
     * is called twice: to report the removal of the old element and the addition of the new one.
     *
     * The subscription is removed when the given [lifetime] expires.
     */
    fun adviseAddRemove(lifetime: Lifetime, handler: (AddRemove, Int, V) -> Unit) {
        advise(lifetime) { when (it) {
            is IViewableList.Event.Add -> handler(AddRemove.Add, it.index, it.newValue)
            is IViewableList.Event.Update -> {
                handler(AddRemove.Remove, it.index, it.oldValue)
                handler(AddRemove.Add, it.index, it.newValue)
            }
            is IViewableList.Event.Remove -> handler(AddRemove.Remove, it.index, it.oldValue)
        }}
    }

    /**
     * Adds a subscription to changes of the contents of the list.
     *
     * When [handler] is initially added, it is called receiving all elements currently in the list.
     * Every time an element is added to the list (as a new one or as an update of an existing one),
     * the [handler] is called receiving the new element and index. The [Lifetime] instance passed to the handler
     * expires when the element is removed from the list.
     *
     * The subscription is removed when the given [lifetime] expires.
     */
    override fun view(lifetime: Lifetime, handler: (Lifetime, Pair<Int, V>) -> Unit) =
            view(lifetime) { lt, idx, v -> handler(lt, Pair(idx, v)) }

    /**
     * Adds a subscription to changes of the contents of the list, receiving the index and value as separate parameters.
     *
     * When [handler] is initially added, it is called receiving all elements currently in the list.
     * Every time an element is added to the list (as a new one or as an update of an existing one),
     * the [handler] is called receiving the new element and index. The [Lifetime] instance passed to the handler
     * expires when the element is removed from the list.
     *
     * The subscription is removed when the given [lifetime] expires.
     */
    fun view(lifetime: Lifetime, handler: (Lifetime, Int, V) -> Unit) {
        val lifetimes = mutableListOf<LifetimeDefinition>()

        adviseAddRemove(lifetime) { kind, idx, value ->
            when (kind) {
                AddRemove.Add -> {
                    val ldef = Lifetime.create(lifetime)
                    lifetimes.add(idx, ldef)
                    handler(ldef.lifetime, idx, value)
                }
                AddRemove.Remove -> lifetimes.removeAt(idx).terminate()
            }
        }
    }
}

interface IAsyncViewableMap<K : Any, V: Any> : IViewableMap<K, V>, IAsyncSource<IViewableMap.Event<K, V>>

interface IMutableViewableMap<K : Any, V: Any> : MutableMap<K, V>, IViewableMap<K, V>

interface IMutableViewableList<V : Any> : MutableList<V>, IViewableList<V>


