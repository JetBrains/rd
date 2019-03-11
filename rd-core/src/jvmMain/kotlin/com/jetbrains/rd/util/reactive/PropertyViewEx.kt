package com.jetbrains.rd.util.reactive

import com.jetbrains.rd.util.lifetime.Lifetime

fun <T, K> IPropertyView<T>.switchMap(f: (T) -> IPropertyView<K>) = object : IPropertyView<K> {

    val deduplicator = Property(value)

    override val change: ISource<K> = object : ISource<K> {
        override fun advise(lifetime: Lifetime, handler: (K) -> Unit) {
            //todo make it optimal way via MultiplexingProperty
            this@switchMap.view(lifetime) { innerLt, v ->
                f(v).advise(innerLt) { deduplicator.value = it }
            }
            deduplicator.advise(lifetime, handler)
        }

    }
    override val value: K
        get() = f(this@switchMap.value).value
}