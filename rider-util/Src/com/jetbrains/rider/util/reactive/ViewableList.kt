package com.jetbrains.rider.util.reactive

import com.jetbrains.rider.util.catch
import com.jetbrains.rider.util.lifetime.Lifetime

class ViewableList<T : Any> : IMutableViewableList<T> {
    private val storage: MutableList<T> = arrayListOf()
    private val change = Signal<IViewableList.Event<T>>()

    override fun advise(lifetime: Lifetime, handler: (IViewableList.Event<T>) -> Unit) {
        change.advise(lifetime, handler)
        this.withIndex().forEach { catch { handler(IViewableList.Event(AddRemove.Add, it.value, it.index)) } }
    }

    override fun add(element: T): Boolean {
        val ret = storage.add(element)
        if (ret) change.fire(IViewableList.Event(AddRemove.Add, element, size - 1))
        return ret
    }

    override fun add(index: Int, element: T) {
        storage.add(index, element)
        change.fire(IViewableList.Event(AddRemove.Add, element, index))
    }

    override fun removeAt(index: Int): T {
        val res = storage.removeAt(index)
        change.fire(IViewableList.Event(AddRemove.Remove, res, index))
        return res
    }

    override fun remove(element: T): Boolean {
        val index = storage.indexOf(element)
        if (index == -1) return false
        removeAt(index)
        return true
    }

    override fun set(index: Int, element: T): T {
        val old = storage.set(index, element)
        change.fire(IViewableList.Event(AddRemove.Remove, old, index))
        change.fire(IViewableList.Event(AddRemove.Add, element, index))
        return old
    }

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        val changes = arrayListOf<IViewableList.Event<T>>()
        var idx = index
        for (element in elements) {
            if (storage.contains(element)) continue
            storage.add(idx, element)
            changes.add(IViewableList.Event(AddRemove.Add, element, idx))
            ++idx
        }
        changes.forEach { change.fire(it) }
        return changes.isNotEmpty()
    }

    override fun addAll(elements: Collection<T>): Boolean {
        val changes = arrayListOf<IViewableList.Event<T>>()
        for (element in elements) {
            if (!storage.add(element)) continue
            changes.add(IViewableList.Event(AddRemove.Add, element, size - 1))
        }
        changes.forEach { change.fire(it) }
        return changes.isNotEmpty()
    }

    override fun clear() {
        val changes = arrayListOf<IViewableList.Event<T>>()
        for (i in (storage.size-1) downTo 0) {
            changes.add(IViewableList.Event(AddRemove.Remove, storage[i], i))
        }
        storage.clear()
        changes.forEach { change.fire(it) }
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        var res = false
        val changes = arrayListOf<IViewableList.Event<T>>()
        for (element in elements) {
            val index = storage.indexOf(element)
            if (index == -1) continue
            storage.removeAt(index)
            changes.add(IViewableList.Event(AddRemove.Remove, element, index))
            res = true
        }
        changes.forEach { change.fire(it) }
        return res
    }


    override fun retainAll(elements: Collection<T>): Boolean {
        var res = false
        val iterator = storage.iterator()
        val changes = arrayListOf<IViewableList.Event<T>>()
        var index = 0
        while (iterator.hasNext()) {
            val value = iterator.next()
            if (!elements.contains(value)) {
                changes.add(IViewableList.Event(AddRemove.Remove, value, index))
                iterator.remove()
                res = true
            } else {
                ++index
            }
        }
        changes.forEach { change.fire(it) }
        return res
    }

    override val size: Int get() = storage.size
    override fun contains(element: T): Boolean = storage.contains(element)
    override fun containsAll(elements: Collection<T>): Boolean = storage.containsAll(elements)
    override fun get(index: Int): T = storage[index]
    override fun indexOf(element: T): Int = storage.indexOf(element)
    override fun isEmpty(): Boolean = storage.isEmpty()
    override fun iterator(): MutableIterator<T> = storage.iterator()
    override fun lastIndexOf(element: T): Int = storage.lastIndexOf(element)
    override fun listIterator(): MutableListIterator<T> = storage.listIterator()
    override fun listIterator(index: Int): MutableListIterator<T> = storage.listIterator(index)

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> = throw UnsupportedOperationException()
}