package com.jetbrains.rd.util.collections

import java.util.function.Predicate

class SynchronizedSet<T> : MutableSet<T> {
    companion object {
        private val PRESENT = Any()
    }

    private val map = SynchronizedMap<T, Any>()

    override fun add(element: T): Boolean {
        return map.put(element, PRESENT) == null
    }

    override fun addAll(elements: Collection<T>): Boolean {
        var added = false
        elements.forEach {
            if (add(it))
                added = true
        }

        return added
    }

    override val size: Int
        get() = map.size

    override fun clear() {
        map.clear()
    }

    override fun isEmpty(): Boolean {
        return map.isEmpty()
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        return map.keys.containsAll(elements)
    }

    override fun contains(element: T): Boolean {
        return map.containsKey(element)
    }

    override fun iterator(): MutableIterator<T> {
        val iterator = map.iterator()

        return object : MutableIterator<T> {
            override fun hasNext(): Boolean = iterator.hasNext()
            override fun next(): T = iterator.next().key
            override fun remove() = iterator.remove()
        }
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        return map.keys.retainAll(elements)
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        var removed = false
        elements.forEach {
            if (remove(it))
                removed = true
        }

        return removed
    }

    override fun remove(element: T): Boolean {
        return map.remove(element) == PRESENT
    }

    override fun removeIf(filter: Predicate<in T>): Boolean {
        return map.removeIf { filter.test(it.key) }
    }
}