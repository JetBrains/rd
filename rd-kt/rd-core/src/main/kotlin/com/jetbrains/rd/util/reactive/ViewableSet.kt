package com.jetbrains.rd.util.reactive

import com.jetbrains.rd.util.catch
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.threading.AdviseToAdviseOnSynchronizerImpl
import com.jetbrains.rd.util.threading.adviseOn
import com.jetbrains.rd.util.threading.modifyAndFireChange

class ViewableSet<T : Any>(private val set: MutableSet<T> = LinkedHashSet<T>()) : IMutableViewableSet<T> {
    private val adviseToAdviseOnSynchronizer = AdviseToAdviseOnSynchronizerImpl()

    override fun add(element: T): Boolean {
        adviseToAdviseOnSynchronizer.modifyAndFireChange(change) {
            if (set.add(element))
                IViewableSet.Event(AddRemove.Add, element)
            else
                return false
        }

        return true
    }

    private inline fun bulkOr(elements: Collection<T>, fn: (T) -> Boolean) = 
            elements.fold(false) { acc, elt -> acc or fn(elt) }

    override fun addAll(elements: Collection<T>) = bulkOr(elements) { add(it) }

    override fun clear() {
        with(iterator()) {
            while (hasNext()) {
                next()
                remove()
            }
        }
    }

    override fun adviseOn(lifetime: Lifetime, scheduler: IScheduler, handler: (IViewableSet.Event<T>) -> Unit) {
        adviseToAdviseOnSynchronizer.adviseOn(this, lifetime, scheduler, handler)
    }

    override fun iterator(): MutableIterator<T> {
        return object : MutableIterator<T> {
            val delegate = set.iterator()
            var current: T? = null
            override fun remove() {
                adviseToAdviseOnSynchronizer.modifyAndFireChange(change) {
                    delegate.remove()
                    IViewableSet.Event(AddRemove.Remove, current!!)
                }
            }

            override fun hasNext(): Boolean = delegate.hasNext()
            override fun next(): T = delegate.next().apply { current = this }
        }
    }

    override fun remove(element: T): Boolean {
        adviseToAdviseOnSynchronizer.modifyAndFireChange(change) {
            if (set.remove(element)) {
                IViewableSet.Event(AddRemove.Remove, element)
            } else {
                return false
            }
        }

        return true
    }

    override fun removeAll(elements: Collection<T>) = bulkOr(elements) { remove(it) }

    override fun retainAll(elements: Collection<T>): Boolean {
        val iterator = iterator()
        var modified = false
        while (iterator.hasNext()) {
            if (!elements.contains(iterator.next())) {
                iterator.remove()
                modified = true
            }
        }
        return modified
    }


    override val change = Signal<IViewableSet.Event<T>>()

    override fun advise(lifetime: Lifetime, handler: (IViewableSet.Event<T>) -> Unit) {
        forEach { catch { handler(IViewableSet.Event(AddRemove.Add, it)) } }
        change.advise(lifetime, handler)
    }

    override val size: Int
        get() = set.size

    override fun contains(element: T): Boolean = set.contains(element)

    override fun containsAll(elements: Collection<T>): Boolean = set.containsAll(elements)

    override fun isEmpty(): Boolean = set.isEmpty()

}
