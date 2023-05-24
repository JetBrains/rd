package com.jetbrains.rd.util.collections

import java.util.function.*
import java.util.function.Function
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class SynchronizedMap<TK, TV> : MutableMap<TK, TV>, MutableSet<MutableMap.MutableEntry<TK, TV>> {
    private var map = LinkedHashMap<TK, TV>()
    private val locker = Any()
    private var isUnderReadingCount = 0

    @Volatile
    private var valuesLazy: ValueCollection? = null

    @Volatile
    private var keysLazy: KeySet? = null

    override val entries: MutableSet<MutableMap.MutableEntry<TK, TV>>
        get() = this

    override val size: Int
        get() = synchronized(locker) {
            map.size
        }

    override val values: MutableCollection<TV>
        get() {
            val values = valuesLazy
            if (values != null) return values

            return synchronized(locker) {
                var values = valuesLazy
                if (values != null) {
                    values
                } else {
                    values = ValueCollection()
                    valuesLazy = values
                    values
                }
            }
        }

    override val keys: MutableSet<TK>
        get() {
            val keys = keysLazy
            if (keys != null) return keys

            return synchronized(locker) {
                var keys = keysLazy
                if (keys != null) {
                    keys
                } else {
                    keys = KeySet()
                    keysLazy = keys
                    keys
                }
            }
        }

    override fun clear() {
        synchronized(locker) {
            getOrCloneMapNoLock().clear()
        }
    }

    override fun isEmpty() = size == 0

    override fun remove(key: TK): TV? {
        return synchronized(locker) {
            getOrCloneMapNoLock().remove(key)
        }
    }

    override fun putAll(from: Map<out TK, TV>) {
        synchronized(locker) {
            getOrCloneMapNoLock().putAll(from)
        }
    }

    override fun put(key: TK, value: TV): TV? {
        return synchronized(locker) {
            getOrCloneMapNoLock().put(key, value)
        }
    }

    override fun get(key: TK): TV? {
        return synchronized(locker) {
            map[key]
        }
    }

    override fun containsValue(value: TV): Boolean {
        return synchronized(locker) {
            map.containsValue(value)
        }
    }

    override fun containsKey(key: TK): Boolean {
        return synchronized(locker) {
            map.containsKey(key)
        }
    }

    override fun remove(key: TK, value: TV): Boolean {
        return synchronized(locker) {
            getOrCloneMapNoLock().remove(key, value)
        }
    }

    override fun compute(key: TK, remappingFunction: BiFunction<in TK, in TV?, out TV?>): TV? {
        var oldValue = get(key)
        while (true) {
            val newValue = remappingFunction.apply(key, oldValue)

            synchronized(locker) {
                val currentValue = map[key]
                if (currentValue != oldValue) {
                    oldValue = currentValue
                    return@synchronized // continue
                }

                return if (newValue == null) {
                    // delete mapping
                    if (oldValue != null) {
                        // something to remove
                        remove(key)
                        null
                    } else {
                        // nothing to do. Leave things as they were.
                        null
                    }
                } else {
                    // add or replace old mapping
                    put(key, newValue)
                    newValue
                }
            }
        }
    }

    override fun computeIfAbsent(key: TK, mappingFunction: Function<in TK, out TV>): TV {
        val oldValue = get(key)
        if (oldValue != null) return oldValue

        val newValue = mappingFunction.apply(key)

        synchronized(locker) {
            val oldValue = map[key]
            if (oldValue != null)
                return oldValue

            put(key, newValue)
            return newValue
        }
    }

    override fun computeIfPresent(key: TK, remappingFunction: BiFunction<in TK, in TV, out TV?>): TV? {
        var oldValue = get(key)
        while (true) {
            if (oldValue == null) return null

            val newValue = remappingFunction.apply(key, oldValue)

            synchronized(locker) {
                val currentValue = map[key]
                if (currentValue != oldValue) {
                    oldValue = currentValue
                    return@synchronized // continue
                }

                return if (newValue == null) {
                    // delete mapping
                    remove(key)
                    null
                } else {
                    // add or replace old mapping
                    put(key, newValue)
                    newValue
                }
            }
        }
    }

    override fun merge(key: TK, value: TV, remappingFunction: BiFunction<in TV, in TV, out TV?>): TV? {
        var oldValue = get(key)
        while (true) {
            val newValue = if (oldValue == null) value else remappingFunction.apply(oldValue, value)

            synchronized(locker) {
                val currentValue = map[key]
                if (currentValue != oldValue) {
                    oldValue = currentValue
                    return@synchronized // continue
                }

                if (newValue == null) {
                    remove(key)
                } else {
                    put(key, newValue)
                }
                return newValue
            }
        }
    }

    override fun putIfAbsent(key: TK, value: TV): TV? {
        return synchronized(locker) {
            super.putIfAbsent(key, value)
        }
    }

    override fun replace(key: TK, oldValue: TV, newValue: TV): Boolean {
        return synchronized(locker) {
            super.replace(key, oldValue, newValue)
        }
    }

    override fun replace(key: TK, value: TV): TV? {
        return synchronized(locker) {
            super.replace(key, value)
        }
    }

    @OptIn(ExperimentalContracts::class)
    private fun updateState(getNewMap: (HashMap<TK, TV>) -> LinkedHashMap<TK, TV>) {
        contract {
            callsInPlace(getNewMap, InvocationKind.AT_LEAST_ONCE)
        }

        while (true) {
            val localMap = synchronized(locker) {
                isUnderReadingCount++
                map
            }

            try {
                val newMap = getNewMap(localMap)

                synchronized(locker) {
                    if (localMap === map) {

                        assert(map !== newMap)

                        map = newMap
                        assert(isUnderReadingCount > 0)
                        isUnderReadingCount = 0
                        return
                    }
                }

            } catch (e: Throwable) {

                if (localMap === map) {
                    synchronized(locker) {
                        if (localMap === map) {
                            val count = --isUnderReadingCount
                            assert(count >= 0)
                        }
                    }
                }

                throw e
            }
        }
    }

    override fun replaceAll(function: BiFunction<in TK, in TV, out TV>) {
        updateState { snapshot ->
            LinkedHashMap<TK, TV>(snapshot.size).also { newMap ->
                snapshot.forEach { (key, value) ->
                    newMap[key] = function.apply(key, value)
                }
            }
        }
    }

    override fun forEach(action: BiConsumer<in TK, in TV>) {
        entries.forEach {
            action.accept(it.key, it.value)
        }
    }

    override fun getOrDefault(key: TK, defaultValue: TV): TV {
        return synchronized(locker) {
            map[key] ?: defaultValue
        }
    }

    override fun add(element: MutableMap.MutableEntry<TK, TV>): Boolean {
        return put(element.key, element.value) == null
    }

    override fun addAll(elements: Collection<MutableMap.MutableEntry<TK, TV>>): Boolean {
        var added = false
        elements.forEach {
            if (add(it)) added = true
        }

        return added
    }

    private inline fun underReading(action: (LinkedHashMap<TK, TV>) -> Unit) {
        val localMap = synchronized(locker) {
            isUnderReadingCount++
            map
        }

        try {
            action(localMap)
        } finally {

            synchronized(locker) {
                if (localMap === map) {
                    val count = --isUnderReadingCount
                    assert(count >= 0)
                }
            }
        }
    }

    override fun iterator(): MutableIterator<MutableMap.MutableEntry<TK, TV>> {
        val iterator = iterator {
            underReading { snapshot ->
                for (entry in snapshot) {
                    yield(entry)
                }
            }
        }

        return object : MutableIterator<MutableMap.MutableEntry<TK, TV>> {
            private var lastEntry: MutableMap.MutableEntry<TK, TV>? = null

            override fun hasNext() = iterator.hasNext()

            override fun next(): MutableMap.MutableEntry<TK, TV> {
                val value = iterator.next()
                lastEntry = value
                return value
            }

            override fun remove() {
                remove(lastEntry ?: throw IllegalStateException())
            }
        }
    }

    override fun remove(element: MutableMap.MutableEntry<TK, TV>): Boolean {
        return synchronized(locker) {
            getOrCloneMapNoLock().entries.remove(element)
        }
    }

    override fun forEach(action: Consumer<in MutableMap.MutableEntry<TK, TV>>?) {
        action ?: return

        for (pair in this) {
            action.accept(pair)
        }
    }

    override fun removeAll(elements: Collection<MutableMap.MutableEntry<TK, TV>>): Boolean {
        return synchronized(locker) {
            getOrCloneMapNoLock().entries.removeAll(elements)
        }
    }

    override fun retainAll(elements: Collection<MutableMap.MutableEntry<TK, TV>>): Boolean {
        return synchronized(locker) {
            getOrCloneMapNoLock().entries.retainAll(elements)
        }
    }

    override fun contains(element: MutableMap.MutableEntry<TK, TV>): Boolean {
        synchronized(locker) {
            return map.entries.contains(element)
        }
    }

    override fun containsAll(elements: Collection<MutableMap.MutableEntry<TK, TV>>): Boolean {
        synchronized(locker) {
            return map.entries.containsAll(elements)
        }
    }

    override fun removeIf(filter: Predicate<in MutableMap.MutableEntry<TK, TV>>): Boolean {
        var removed: Boolean

        updateState { snapshot ->
            removed = false

            LinkedHashMap<TK, TV>(snapshot.size).also { newMap ->
                snapshot.entries.forEach { entry ->
                    if (filter.test(entry)) {
                        removed = true
                    } else {
                        newMap[entry.key] = entry.value
                    }
                }
            }
        }

        return removed
    }

    private inner class KeySet : MutableSet<TK> {
        override fun add(element: TK): Boolean {
            return synchronized(locker) {
                getOrCloneMapNoLock().keys.add(element)
            }
        }

        override fun addAll(elements: Collection<TK>): Boolean {
            return synchronized(locker) {
                getOrCloneMapNoLock().keys.addAll(elements)
            }
        }

        override val size: Int
            get() = this@SynchronizedMap.size

        override fun clear() {
            return synchronized(locker) {
                getOrCloneMapNoLock().keys.clear()
            }
        }

        override fun isEmpty(): Boolean {
            return this@SynchronizedMap.isEmpty()
        }

        override fun containsAll(elements: Collection<TK>): Boolean {
            return elements.all { contains(it) }
        }

        override fun contains(element: TK): Boolean {
            return this@SynchronizedMap.containsKey(element)
        }

        override fun iterator(): MutableIterator<TK> {
            val iterator = this@SynchronizedMap.iterator()

            return object : MutableIterator<TK> {
                override fun hasNext() = iterator.hasNext()
                override fun next() = iterator.next().key
                override fun remove() = iterator.remove()
            }
        }

        override fun retainAll(elements: Collection<TK>): Boolean {
            return synchronized(locker) {
                getOrCloneMapNoLock().keys.retainAll(elements)
            }
        }

        override fun removeAll(elements: Collection<TK>): Boolean {
            return synchronized(locker) {
                getOrCloneMapNoLock().keys.removeAll(elements)
            }
        }

        override fun remove(element: TK): Boolean {
            return synchronized(locker) {
                getOrCloneMapNoLock().keys.remove(element)
            }
        }
    }

    private inner class ValueCollection : MutableCollection<TV> {
        override val size: Int
            get() = this@SynchronizedMap.size

        override fun clear() {
            return synchronized(locker) {
                getOrCloneMapNoLock().values.clear()
            }
        }

        override fun addAll(elements: Collection<TV>): Boolean {
            return synchronized(locker) {
                getOrCloneMapNoLock().values.addAll(elements)
            }
        }

        override fun add(element: TV): Boolean {
            return synchronized(locker) {
                getOrCloneMapNoLock().values.add(element)
            }
        }

        override fun isEmpty(): Boolean {
            return this@SynchronizedMap.isEmpty()
        }

        override fun iterator(): MutableIterator<TV> {
            val iterator = this@SynchronizedMap.iterator()

            return object : MutableIterator<TV> {
                override fun hasNext() = iterator.hasNext()
                override fun next() = iterator.next().value
                override fun remove() = iterator.remove()
            }
        }

        override fun retainAll(elements: Collection<TV>): Boolean {
            return synchronized(locker) {
                getOrCloneMapNoLock().values.retainAll(elements)
            }
        }

        override fun removeAll(elements: Collection<TV>): Boolean {
            return synchronized(locker) {
                getOrCloneMapNoLock().values.removeAll(elements)
            }
        }

        override fun remove(element: TV): Boolean {
            return synchronized(locker) {
                getOrCloneMapNoLock().values.remove(element)
            }
        }

        override fun containsAll(elements: Collection<TV>): Boolean {
            return synchronized(locker) {
                map.values.containsAll(elements)
            }
        }

        override fun contains(element: TV): Boolean {
            return synchronized(locker) {
                map.values.contains(element)
            }
        }
    }

    private fun getOrCloneMapNoLock(): LinkedHashMap<TK, TV> {
        var localMap = map
        if (isUnderReadingCount > 0) {
            localMap = LinkedHashMap(localMap)
            isUnderReadingCount = 0
            map = localMap
            return localMap
        }

        return localMap
    }
}