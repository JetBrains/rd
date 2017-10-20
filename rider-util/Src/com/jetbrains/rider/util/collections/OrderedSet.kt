package com.jetbrains.rider.util.collections

import java.util.*
import java.util.function.Predicate
import java.util.function.UnaryOperator

class OrderedSet<T>() : MutableList<T>, ArrayList<T>() {
    private val containedElements = HashSet<T>()

    inline private fun doIfUnique(element: T, crossinline action: (T) -> Unit): Boolean {
        if (containedElements.contains(element)) return false
        action(element)
        containedElements.add(element)
        return true
    }

    override fun add(element: T): Boolean {
        return doIfUnique(element, { super.add(element) })
    }

    override fun add(index: Int, element: T) {
        val ret = doIfUnique(element, { super.add(index, element) })
        if (ret.not()) throw IllegalArgumentException("The element has already contained.")
    }

    override fun removeAt(index: Int): T {
        val removed = super.removeAt(index)
        containedElements.remove(removed)
        return removed
    }

    override fun remove(element: T): Boolean {
        val ret = super.remove(element)
        containedElements.remove(element)
        return ret
    }

    override fun contains(element: T): Boolean {
        return containedElements.contains(element)
    }

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        val uniqueElements = elements.filter { containedElements.add(it) }
        return super.addAll(index, uniqueElements)
    }

    override fun addAll(elements: Collection<T>): Boolean {
        val uniqueElements = elements.filter { containedElements.add(it) }
        return super.addAll(uniqueElements)
    }

    override fun clear() {
        super.clear()
        containedElements.clear()
    }

    override fun set(index: Int, element: T): T {
        val old = removeAt(index)
        add(index, element)
        return old
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        containedElements.retainAll(elements)
        return super.retainAll(elements)
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        containedElements.removeAll(elements)
        return super.removeAll(elements)
    }

    override fun removeRange(fromIndex: Int, toIndex: Int) {
        for (i in fromIndex..toIndex)
            containedElements.remove(this[i])
        super.removeRange(fromIndex, toIndex)
    }

    override fun containsAll(elements: Collection<T>): Boolean = containedElements.containsAll(elements)

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> = throw UnsupportedOperationException()
    override fun removeIf(filter: Predicate<in T>): Boolean { throw UnsupportedOperationException() }
    override fun replaceAll(operator: UnaryOperator<T>) { throw UnsupportedOperationException() }
}