package com.jetbrains.rd.framework.base

import com.jetbrains.rd.framework.IRdDynamic
import com.jetbrains.rd.framework.RdContext
import com.jetbrains.rd.util.reactive.IViewableMap

/**
 * A collection that automatically maps values to keys from RdContext's value set
 * Key-value pairs in this map are automatically managed based on possible values of RdContext
 *
 * As context value sets are protocol-specific, this map will behave differently depending on whether or not it's bound to a [com.jetbrains.rd.framework.IProtocol]
 * An unbound map will automatically create mappings for all context values it's accessed with. When a map is bound later, all values not present in protocol value set will be silently dropped.
 *
 * @see [com.jetbrains.rd.framework.impl.ProtocolContexts.getValueSet]
 */
interface IPerContextMap<K : Any, out V: Any>: IViewableMap<K, V>, IRdDynamic {
    /**
     * The context that is used by this map. Must be heavy.
     */
    val context: RdContext<K>

    /**
     * Gets the value associated with specified context value, or null if none is associated
     * When this map is not bound, this will automatically create a new mapping instead of returning null
     */
    override operator fun get(key: K): V?

    /**
     * Gets the value associated with current context value, equivalent to get(context.value)
     * If the context doesn't have a value set, or contexts's protocol value set does not contain the current context value, this will throw an exception
     */
    fun getForCurrentContext(): V
}