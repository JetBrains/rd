package com.jetbrains.rd.framework.base

import com.jetbrains.rd.framework.IRdDynamic
import com.jetbrains.rd.framework.RdContext
import com.jetbrains.rd.util.reactive.IViewableMap

/**
 * A collection that automatically maps values to keys from RdContextKey's value set
 * Key-value pairs in this map are automatically managed based on possible values of RdContextKey
 *
 * As context key value sets are protocol-specific, this map will behave differently depending on whether or not it's bound to a [com.jetbrains.rd.framework.IProtocol]
 * An unbound map will automatically create mappings for all context values it's accessed with. When a map is bound later, all values not present in protocol value set will be silently dropped.
 *
 * @see [com.jetbrains.rd.framework.impl.ProtocolContexts.getValueSet]
 */
interface IPerContextMap<K : Any, out V: Any>: IViewableMap<K, V>, IRdDynamic {
    /**
     * The context key that is used by this map. Must be heavy.
     */
    val key: RdContext<K>

    /**
     * Gets the value associated with specified context value, or null if none is associated
     * When this map is not bound, this will automatically create a new mapping instead of returning null
     */
    override operator fun get(key: K): V?

    /**
     * Gets the value associated with current context value, equivalent to get(key.value)
     * If the context key doesn't have a value set, or key's protocol value set does not contain the current context value, this will throw an exception
     */
    fun getForCurrentContext(): V
}