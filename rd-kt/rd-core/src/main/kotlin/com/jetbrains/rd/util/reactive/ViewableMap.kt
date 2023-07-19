package com.jetbrains.rd.util.reactive

import com.jetbrains.rd.util.catch
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.put
import com.jetbrains.rd.util.reactive.IViewableMap.Event
import com.jetbrains.rd.util.threading.AdviseToAdviseOnSynchronizerImpl
import com.jetbrains.rd.util.threading.adviseOn
import com.jetbrains.rd.util.threading.modifyAndFireChange
import com.jetbrains.rd.util.threading.modifyAndFireChanges

class ViewableMap<K : Any, V : Any>(private val map: MutableMap<K, V> = LinkedHashMap()) : IMutableViewableMap<K, V> {

    override val change = Signal<Event<K, V>>()
    private val adviseToAdviseOnSynchronizer = AdviseToAdviseOnSynchronizerImpl()

    override fun advise(lifetime: Lifetime, handler: (Event<K, V>) -> Unit) {
        change.advise(lifetime, handler)
        this.forEach { catch { handler(Event.Add(it.key, it.value)) } }
    }

    override fun putAll(from: Map<out K, V>) {
        from.forEach { put(it) }
    }

    override fun put(key: K, value: V): V? {
        var oldval: V?
        adviseToAdviseOnSynchronizer.modifyAndFireChange(change) {
            oldval = map.put(key, value)

            if (oldval != null) {
                Event.Update(key, oldval!!, value)
            } else {
                Event.Add(key, value)
            }
        }
        return oldval
    }

    override fun remove(key: K): V? {
        var oldval: V?
        adviseToAdviseOnSynchronizer.modifyAndFireChange(change) {
            oldval = map.remove(key)
            if (oldval == null) return null
            else Event.Remove(key, oldval!!)
        }

        return oldval
    }

    override fun remove(key: K, value: V): Boolean {
        adviseToAdviseOnSynchronizer.modifyAndFireChange(change) {
            if (map.remove(key, value))
                Event.Remove(key, value)
            else
                return false
        }

        return true
    }

    override fun clear() {
        adviseToAdviseOnSynchronizer.modifyAndFireChanges(change) {
            val changes = arrayListOf<Event<K, V>>()
            val iterator = map.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                changes.add(Event.Remove(entry.key, entry.value))
                iterator.remove()
            }

            changes
        }
    }

    override val keys: MutableSet<K> get() = map.keys
    override val values: MutableCollection<V> get() = map.values
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> get() = map.entries
    override val size: Int get() = map.size
    override fun isEmpty(): Boolean = map.isEmpty()
    override fun containsKey(key: K): Boolean = map.containsKey(key)
    override fun containsValue(value: V): Boolean = map.containsValue(value)
    override fun get(key: K): V? = map[key]

    override fun adviseOn(lifetime: Lifetime, scheduler: IScheduler, handler: (Event<K, V>) -> Unit) {
        adviseToAdviseOnSynchronizer.adviseOn(this, lifetime, scheduler, handler)
    }
}


