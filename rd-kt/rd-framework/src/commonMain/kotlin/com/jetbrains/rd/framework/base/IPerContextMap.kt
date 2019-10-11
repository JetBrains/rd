package com.jetbrains.rd.framework.base

import com.jetbrains.rd.framework.RdContextKey
import com.jetbrains.rd.util.lifetime.Lifetime

interface IPerContextMap<K : Any, out V: Any> {
    val key: RdContextKey<K>
    fun view(lifetime: Lifetime, handler: (Lifetime, Map.Entry<K, V>) -> Unit)
    fun view(lifetime: Lifetime, handler: (Lifetime, K, V) -> Unit)
    operator fun get(key: K): V?
    fun getForCurrentContext(): V
}