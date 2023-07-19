package com.jetbrains.rd.util.reactive

import com.jetbrains.rd.util.catch
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.isAlive
import com.jetbrains.rd.util.threading.AdviseToAdviseOnSynchronizerImpl
import com.jetbrains.rd.util.threading.adviseOn
import com.jetbrains.rd.util.threading.modifyAndFireChange
import com.jetbrains.rd.util.threading.modifyAndFireChanges

class ViewableList<T : Any>(private val storage: MutableList<T> = mutableListOf()) : IMutableViewableList<T> {
    override val change = Signal<IViewableList.Event<T>>()
    private val adviseToAdviseOnSynchronizer = AdviseToAdviseOnSynchronizerImpl()

    override fun advise(lifetime: Lifetime, handler: (IViewableList.Event<T>) -> Unit) {
        if (!lifetime.isAlive) return

        change.advise(lifetime, handler)
        this.withIndex().forEach { catch { handler(IViewableList.Event.Add(it.index, it.value)) } }
    }

    override fun adviseOn(lifetime: Lifetime, scheduler: IScheduler, handler: (IViewableList.Event<T>) -> Unit) {
        adviseToAdviseOnSynchronizer.adviseOn(this, lifetime, scheduler, handler)
    }

    override fun add(element: T): Boolean {
        adviseToAdviseOnSynchronizer.modifyAndFireChange(change) {
            storage.add(element)
            IViewableList.Event.Add(size - 1, element)
        }

        return true
    }

    override fun add(index: Int, element: T) {
        adviseToAdviseOnSynchronizer.modifyAndFireChange(change) {
            storage.add(index, element)
            IViewableList.Event.Add(index, element)
        }
    }

    override fun removeAt(index: Int): T {
        var res: T
        adviseToAdviseOnSynchronizer.modifyAndFireChange(change) {
            res = storage.removeAt(index)
            IViewableList.Event.Remove(index, res)
        }
        return res
    }

    override fun remove(element: T): Boolean {
        val index = storage.indexOf(element)
        if (index == -1) return false

        adviseToAdviseOnSynchronizer.modifyAndFireChange(change) {
            storage.removeAt(index)
            IViewableList.Event.Remove(index, removeAt(index))
        }

        return true
    }

    override fun set(index: Int, element: T): T {
        var old: T
        adviseToAdviseOnSynchronizer.modifyAndFireChange(change) {
            old = storage.set(index, element)
            IViewableList.Event.Update(index, old, element)
        }

        return old
    }

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        val changes = arrayListOf<IViewableList.Event<T>>()

        adviseToAdviseOnSynchronizer.modifyAndFireChanges(change) {
            var idx = index
            for (element in elements) {
                storage.add(idx, element)
                changes.add(IViewableList.Event.Add(idx, element))
                ++idx
            }

            changes
        }

        return changes.isNotEmpty()
    }

    override fun addAll(elements: Collection<T>): Boolean {
        val changes = arrayListOf<IViewableList.Event<T>>()
        adviseToAdviseOnSynchronizer.modifyAndFireChanges(change) {
            for (element in elements) {
                storage.add(element)
                changes.add(IViewableList.Event.Add(size - 1, element))
            }

            changes
        }

        return changes.isNotEmpty()
    }

    override fun clear() {
        val changes = arrayListOf<IViewableList.Event<T>>()
        adviseToAdviseOnSynchronizer.modifyAndFireChanges(change) {
            for (i in (storage.size-1) downTo 0) {
                changes.add(IViewableList.Event.Remove(i, storage[i]))
            }
            storage.clear()

            changes
        }
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        return filterElementsInplace(elements) { index, elementsSet -> storage[index] in elementsSet }
    }

    private inline fun filterElementsInplace(elements: Collection<T>, predicate: (Int, Set<T>) -> Boolean): Boolean {
        val changes = arrayListOf<IViewableList.Event<T>>()
        adviseToAdviseOnSynchronizer.modifyAndFireChanges(change) {
            val elementsSet = elements.toSet()
            for (index in storage.lastIndex downTo 0) {
                if (predicate(index, elementsSet)) {
                    changes.add(IViewableList.Event.Remove(index, storage.removeAt(index)))
                }
            }

            changes
        }

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

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> = throw UnsupportedOperationException()

    private inner class MyIterator(val baseIterator: MutableListIterator<T>): MutableListIterator<T> by baseIterator {
        override fun add(element: T) {
            adviseToAdviseOnSynchronizer.modifyAndFireChange(change) {
                baseIterator.add(element)
                IViewableList.Event.Add(previousIndex(), element)
            }
        }

        override fun remove() {

            adviseToAdviseOnSynchronizer.modifyAndFireChange(change, ) {
                val index = previousIndex()
                val element = storage[index]
                baseIterator.remove()
                IViewableList.Event.Remove(index, element)
            }
        }

        override fun set(element: T) {

            adviseToAdviseOnSynchronizer.modifyAndFireChange(change) {
                val index = previousIndex()
                val oldElement = storage[index]
                baseIterator.set(element)
                IViewableList.Event.Update(index, oldElement, element)
            }
        }

        override fun previous(): T {
            throw UnsupportedOperationException("Can't use previous here")
        }
    }
}