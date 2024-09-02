package com.jetbrains.rd.util.reactive

import com.jetbrains.rd.util.catch
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.isAlive
import java.util.Objects

class ViewableList<T : Any>(private val storage: MutableList<T> = mutableListOf()) : IMutableViewableList<T> {
    override val change = Signal<IViewableList.Event<T>>()

    override fun advise(lifetime: Lifetime, handler: (IViewableList.Event<T>) -> Unit) {
        if (!lifetime.isAlive) return

        change.advise(lifetime, handler)
        this.withIndex().forEach { catch { handler(IViewableList.Event.Add(it.index, it.value)) } }
    }

    override fun add(element: T): Boolean {
        storage.add(element)
        change.fire(IViewableList.Event.Add(size - 1, element))
        return true
    }

    override fun add(index: Int, element: T) {
        storage.add(index, element)
        change.fire(IViewableList.Event.Add(index, element))
    }

    override fun removeAt(index: Int): T {
        val res = storage.removeAt(index)
        change.fire(IViewableList.Event.Remove(index, res))
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
        change.fire(IViewableList.Event.Update(index, old, element))
        return old
    }

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        val changes = arrayListOf<IViewableList.Event<T>>()
        var idx = index
        for (element in elements) {
            storage.add(idx, element)
            changes.add(IViewableList.Event.Add(idx, element))
            ++idx
        }
        changes.forEach { change.fire(it) }
        return changes.isNotEmpty()
    }

    private fun addAll(iterator: Iterator<T>): Boolean {
        val changes = arrayListOf<IViewableList.Event<T>>()
        for (element in iterator) {
            storage.add(element)
            changes.add(IViewableList.Event.Add(size - 1, element))
        }
        changes.forEach { change.fire(it) }
        return changes.isNotEmpty()
    }

    override fun addAll(elements: Collection<T>) = addAll(elements.iterator())

    override fun clear() {
        val changes = arrayListOf<IViewableList.Event<T>>()
        for (i in (storage.size-1) downTo 0) {
            changes.add(IViewableList.Event.Remove(i, storage[i]))
        }
        storage.clear()
        changes.forEach { change.fire(it) }
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        return filterElementsInplace(elements) { index, elementsSet -> storage[index] in elementsSet }
    }

    fun removeRange(fromIndex: Int, toIndex: Int) {
        when (toIndex - fromIndex) {
            0 -> Unit
            1 -> removeAt(fromIndex)
            else -> removeRangeSlow(fromIndex, toIndex)
        }
    }

    private fun removeRangeSlow(fromIndex: Int, toIndex: Int) {
        val changes = buildList<IViewableList.Event<T>>(toIndex - fromIndex) {
            for (i in (toIndex - 1) downTo fromIndex) {
                add(IViewableList.Event.Remove(i, storage[i]))
            }
        }
        storage.subList(fromIndex, toIndex).clear()
        changes.forEach { change.fire(it) }
    }

    private inline fun filterElementsInplace(elements: Collection<T>, predicate: (Int, Set<T>) -> Boolean): Boolean {
        val elementsSet = elements.toSet()
        val changes = arrayListOf<IViewableList.Event<T>>()
        for (index in storage.lastIndex downTo 0) {
            if (predicate(index, elementsSet)) {
                changes.add(IViewableList.Event.Remove(index, storage.removeAt(index)))
            }
        }
        changes.forEach { change.fire(it) }
        return changes.isNotEmpty()
    }


    override fun retainAll(elements: Collection<T>): Boolean {
        return filterElementsInplace(elements) { index, elementsSet -> storage[index] !in elementsSet }
    }

    override val size: Int get() = storage.size
    override fun contains(element: T): Boolean = storage.contains(element)
    override fun containsAll(elements: Collection<T>): Boolean = storage.containsAll(elements)
    override fun get(index: Int): T = storage[index]
    override fun indexOf(element: T): Int = storage.indexOf(element)
    override fun isEmpty(): Boolean = storage.isEmpty()
    override fun iterator(): MutableIterator<T> = listIterator()
    override fun lastIndexOf(element: T): Int = storage.lastIndexOf(element)
    override fun listIterator(): MutableListIterator<T> = MyIterator(storage.listIterator())
    override fun listIterator(index: Int): MutableListIterator<T> = MyIterator(storage.listIterator(index))

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> {
        Objects.checkFromToIndex(fromIndex, toIndex, size)
        return MySubList(fromIndex, toIndex - fromIndex)
    }

    /**
     * Synchronizes the viewable list by adding missing elements and removing unmatched elements.
     * If the order of equal values is not changed, then they won't be modified.
     * However, even if equal elements exist in both lists,
     * but order is swapped, then they will be removed and re-added to satisfy the new values order.
     * It helps drastically reduce the number of change events if the collection is unmodified at all
     * or just a few elements are changed compared to the classical approach with 'clear' and 'addAll'.
     *
     * @param newValues the new values to be synced with
    </T> */
    fun sync(newValues: Collection<T>): Boolean {
        if (isEmpty()) {
            return addAll(newValues)
        }

        if (newValues.isEmpty()) {
            clear()
            return true
        }

        val iterator = iterator()
        val newIterator = newValues.iterator()

        var index = 0
        var newValue: T
        while (true) {
            newValue = newIterator.next()
            if (newValue != iterator.next())
            {
                replaceTailSlow(index, newValue, newIterator)
                return true
            }
            ++index
            if (!newIterator.hasNext()) {
                removeRange(index, size)
                return true
            }
            if (!iterator.hasNext()) {
                return addAll(newIterator)
            }
        }
    }

    private fun replaceTailSlow(firstUnmatchedIndex: Int, firstUnmatchedValue: T, newIterator: Iterator<T>) {
        fun matchIndex(items: MutableMap<T, Any>, value: T, fromIndex: Int): Int? {
            val matchedIndex = items.remove(value)
            if (matchedIndex is Int) {
                return if (matchedIndex >= fromIndex) matchedIndex else null
            }
            @Suppress("UNCHECKED_CAST")
            (matchedIndex as? ArrayDeque<Int>)?.let {
                while (matchedIndex.size > 0) {
                    val endIndex = matchedIndex.removeFirst()
                    if (endIndex >= fromIndex) {
                        if (matchedIndex.size > 0) {
                            items[value] = matchedIndex
                        }
                        return endIndex
                    }
                }
            }
            return null
        }

        val items = mutableMapOf<T, Any>()
        var newValue = firstUnmatchedValue
        for (index in firstUnmatchedIndex until size) {
            val item = this[index]
            val itemIndex = items[item]
            @Suppress("UNCHECKED_CAST")
            when (itemIndex) {
                is Int -> items[item] = ArrayDeque<Int>().apply {
                    add(itemIndex)
                    add(index)
                }
                is ArrayDeque<*> -> (itemIndex as ArrayDeque<Int>).add(index)
                null -> items[item] = index
            }
        }

        val changes = ArrayDeque<IViewableList.Event<T>>()
        val originalSize = size
        var insertIndex = firstUnmatchedIndex
        var processedIndex = firstUnmatchedIndex
        var matchedIndex: Any?
        while (true) {
            matchedIndex = matchIndex(items, newValue, processedIndex)
            if (matchedIndex != null) {
                val removeCount = matchedIndex - processedIndex
                if (removeCount > 0) {
                    for (removeIndex in processedIndex until matchedIndex) {
                        changes.addFirst(IViewableList.Event.Remove(removeIndex, storage[removeIndex]))
                    }
                }
                processedIndex = matchedIndex + 1
                storage.add(storage[matchedIndex])
                ++insertIndex
            }
            else {
                changes.add(IViewableList.Event.Add(insertIndex++, newValue))
                storage.add(newValue)
            }
            if (!newIterator.hasNext())
                break
            newValue = newIterator.next()
        }

        // If last new value was matched then we generate remove events after all "add"
        // events so last "remove" event will match the tail for "viewTail" extension property.
        // Otherwise, we keep an "add" event for the last element and generate all "remove" events at the beginning
        if (matchedIndex != null) {
            val addedElementsAdjustment = insertIndex - processedIndex
            for (removeIndex in originalSize - 1 downTo processedIndex) {
                changes.add(IViewableList.Event.Remove(removeIndex + addedElementsAdjustment, storage[removeIndex]))
            }
        }
        else {
            for (removeIndex in processedIndex until originalSize) {
                changes.addFirst(IViewableList.Event.Remove(removeIndex, storage[removeIndex]))
            }
        }

        storage.subList(firstUnmatchedIndex, originalSize).clear()

        changes.forEach { change.fire(it) }
    }

    private inner class MySubList(private val fromIndex: Int, size: Int) : AbstractMutableList<T>() {
        var mySize = size

        override val size get() = mySize

        override fun add(index: Int, element: T) {
            Objects.checkIndex(index, mySize + 1)
            this@ViewableList.add(fromIndex + index, element).also { ++mySize }
        }

        override fun get(index: Int): T {
            Objects.checkIndex(index, mySize)
            return this@ViewableList[index]
        }

        override fun removeAt(index: Int): T {
            Objects.checkIndex(index, mySize)
            return this@ViewableList.removeAt(index).also { --mySize }
        }

        override fun set(index: Int, element: T): T {
            Objects.checkIndex(index, mySize)
            return this@ViewableList.set(index, element)
        }

        override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> {
            Objects.checkFromToIndex(fromIndex, toIndex, mySize)
            return MySubList(this.fromIndex + fromIndex, toIndex - fromIndex)
        }

        override fun clear() {
            this@ViewableList.removeRange(fromIndex, fromIndex + size).also { mySize = 0 }
        }
    }

    private inner class MyIterator(val baseIterator: MutableListIterator<T>): MutableListIterator<T> by baseIterator {
        override fun add(element: T) {
            baseIterator.add(element)
            change.fire(IViewableList.Event.Add(previousIndex(), element))
        }

        override fun remove() {
            val index = previousIndex()
            val element = storage[index]
            baseIterator.remove()
            change.fire(IViewableList.Event.Remove(index, element))
        }

        override fun set(element: T) {
            val index = previousIndex()
            val oldElement = storage[index]
            baseIterator.set(element)
            change.fire(IViewableList.Event.Update(index, oldElement, element))
        }

        override fun previous(): T {
            throw UnsupportedOperationException("Can't use previous here")
        }
    }
}