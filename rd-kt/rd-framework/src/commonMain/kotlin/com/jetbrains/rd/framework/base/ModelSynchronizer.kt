package com.jetbrains.rd.framework.base

import com.jetbrains.rd.framework.Protocol
import com.jetbrains.rd.framework.impl.RdPerContextMap
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.*
import com.jetbrains.rd.util.trace

class ModelSynchronizer(val accepts: (Any?) -> Boolean = { true }) {

    companion object {
        private val logger = Protocol.sublogger("SYNC")
    }

    private fun <T> cloneAndSync(lf: Lifetime, x: T): T = x.deepClonePolymorphic().also { synchronizePolymorphic(lf, x, it) }

    private fun <T> synchronize(lifetime: Lifetime, a: ISignal<T>, b: ISignal<T>) {
        a.flowInto(lifetime, b)
        b.flowInto(lifetime, a)
    }

    private fun <T> synchronize(lifetime: Lifetime, a: IMutablePropertyBase<T>, b: IMutablePropertyBase<T>) {
        val initialSyncIsFromAToB = a is IOptPropertyView<*> && a.hasValue || a is IPropertyView<*>

        if (initialSyncIsFromAToB) {
            a.flowInto(lifetime, b) { cloneAndSync(lifetime, it) }
            b.change.flowInto(lifetime, a) { cloneAndSync(lifetime, it) }
        } else {
            b.flowInto(lifetime, a) { cloneAndSync(lifetime, it) }
            a.change.flowInto(lifetime, b) { cloneAndSync(lifetime, it) }
        }
    }

    private fun <T : Any> synchronize(lifetime: Lifetime, a: IMutableViewableSet<T>, b: IMutableViewableSet<T>) {
        a.forEach {
            b.remove(it)
            b.add(cloneAndSync(lifetime, it))
        }

        b.forEach { if (!a.contains(it)) a.add(cloneAndSync(lifetime, it)) }

        a.change.flowInto(lifetime, b) { cloneAndSync(lifetime, it) }
        b.change.flowInto(lifetime, a) { cloneAndSync(lifetime, it) }
    }

    private fun <K : Any, V : Any> synchronize(lifetime: Lifetime, a: IMutableViewableMap<K, V>, b: IMutableViewableMap<K, V>) {
        a.forEach {
            b[it.key] = cloneAndSync(lifetime, it.value)
        }

        b.forEach {
            if (!a.containsKey(it.key)) a[it.key] = cloneAndSync(lifetime, it.value)
        }

        a.change.flowInto(lifetime, b) { cloneAndSync(lifetime, it) }
        b.change.flowInto(lifetime, a) { cloneAndSync(lifetime, it) }
    }

    private fun <T : Any> synchronize(lifetime: Lifetime, a: IMutableViewableList<T>, b: IMutableViewableList<T>) {
        b.clear()

        a.flowInto(lifetime, b) { cloneAndSync(lifetime, it) }
        b.change.flowInto(lifetime, a) { cloneAndSync(lifetime, it) }
    }

    private fun <T : Any> synchronizeImmutableLists(lifetime: Lifetime, a: List<T>, b: List<T>) {
        require(a.size == b.size) { "Collection initial sizes must be equal, but sizes are different: ${a.size} != ${b.size}" }
        for (i in 0..a.lastIndex) {
            synchronizePolymorphic(lifetime, a[i], b[i])
        }
    }

    private fun <T : Any> synchronizeImmutableArrays(lifetime: Lifetime, a: Array<T>, b: Array<T>) {
        require(a.size == b.size) { "Collection initial sizes must be equal, but sizes are different: ${a.size} != ${b.size}" }
        for (i in 0..a.lastIndex) {
            synchronizePolymorphic(lifetime, a[i], b[i])
        }
    }

    private fun <K : Any, T : RdBindableBase> synchronize(lifetime: Lifetime, a: RdPerContextMap<K, T>, b: RdPerContextMap<K, T>) {
        a.view(lifetime) { lt, (key, value) ->
            if (b.changing)
                return@view

            b[key]?.let { otherValue -> synchronizePolymorphic(lt, value, otherValue) }
        }

        var isLocal = true
        try {
            b.view(lifetime) { lt, (key, value) ->
                if (a.changing || isLocal)
                    return@view

                a[key]?.let { otherValue -> synchronizePolymorphic(lt, value, otherValue) }
            }
        } finally {
            isLocal = false
        }
    }

    @Suppress("UNCHECKED_CAST")
    internal fun synchronizePolymorphic(lifetime: Lifetime, first: Any?, second: Any?) {

        if (first == second || !accepts(first) || !accepts(second))
            return


        else if (first is RdDelegateBase<*> && second is RdDelegateBase<*>) {
            first.delegatedBy.synchronizeWith(lifetime, second.delegatedBy)

        } else if (first is ISignal<*> && second is ISignal<*>) {
            synchronize(lifetime, first as ISignal<Any>, second as ISignal<Any>)

        } else if (first is IMutablePropertyBase<*> && second is IMutablePropertyBase<*>) {
            synchronize(lifetime, first as IMutablePropertyBase<Any>, second as IMutablePropertyBase<Any>)

        } else if (first is IMutableViewableList<*> && second is IMutableViewableList<*>) {
            synchronize(lifetime, first as IMutableViewableList<Any>, second as IMutableViewableList<Any>)

        } else if (first is IMutableViewableSet<*> && second is IMutableViewableSet<*>) {
            synchronize(lifetime, first as IMutableViewableSet<Any>, second as IMutableViewableSet<Any>)

        } else if (first is IMutableViewableMap<*, *> && second is IMutableViewableMap<*, *>) {
            synchronize(lifetime, first as IMutableViewableMap<Any, Any>, second as IMutableViewableMap<Any, Any>)

        } else if (first is RdPerContextMap<*, *> && second is RdPerContextMap<*, *>) {
            synchronize(lifetime, first as RdPerContextMap<Any, RdBindableBase>, second as RdPerContextMap<Any, RdBindableBase>)

        } else if (first is RdBindableBase && second is RdBindableBase) {
            first.synchronizeWith(lifetime, second)

            //collections
        } else if (first is List<*> && second is List<*>) {
            synchronizeImmutableLists(lifetime, first as List<Any>, second as List<Any>)

        } else if (first is Array<*> && second is Array<*>) {
            synchronizeImmutableArrays(lifetime, first as Array<Any>, second as Array<Any>)

        } else
            error("Objects are not mutually synchronizable: 1) $first   2) $second")

        logger.trace { "synchronized 1) $first   2) $second" }
    }
}