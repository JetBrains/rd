package com.jetbrains.rd.util.collections

import java.util.*
import java.util.function.Predicate
import java.util.function.UnaryOperator
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class SynchronizedList<T> : MutableList<T> {
    private var list = mutableListOf<T>()
    private val locker = Any()
    private var isUnderReadingCount = 0

    override val size: Int
        get() = synchronized(locker) {
            list.size
        }

    override fun clear() {
        synchronized(locker) {
            list.clear()
        }
    }

    override fun addAll(elements: Collection<T>): Boolean {
        return synchronized(locker) {
            getOrCloneListNoLock().addAll(elements)
        }
    }

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        return synchronized(locker) {
            getOrCloneListNoLock().addAll(index, elements)
        }
    }

    override fun add(index: Int, element: T) {
        return synchronized(locker) {
            getOrCloneListNoLock().add(index, element)
        }
    }

    override fun add(element: T): Boolean {
        return synchronized(locker) {
            getOrCloneListNoLock().add(element)
        }
    }

    override fun get(index: Int): T {
        return synchronized(locker) {
            list[index]
        }
    }

    override fun isEmpty(): Boolean {
        return synchronized(locker) {
            list.isEmpty()
        }
    }


    override fun iterator(): MutableIterator<T> {
        val iterator = iterator {
            underReading { snapshot ->
                for (element in snapshot) {
                    yield(element)
                }
            }
        }

        return object : MutableIterator<T> {
            override fun hasNext() = iterator.hasNext()
            override fun next(): T = iterator.next()
            override fun remove() = throw UnsupportedOperationException()
        }
    }

    override fun listIterator(): MutableListIterator<T> = listIterator(0)

    override fun listIterator(index: Int): MutableListIterator<T> {
        val snapshot = synchronized(locker) {
            list.drop(index)
        }

        val iterator = snapshot.listIterator()

        return object : MutableListIterator<T> {
            override fun hasNext(): Boolean = iterator.hasNext()
            override fun hasPrevious(): Boolean = iterator.hasPrevious()
            override fun next(): T = iterator.next()
            override fun nextIndex(): Int = iterator.nextIndex()
            override fun previous(): T = iterator.next()
            override fun previousIndex(): Int = iterator.previousIndex()

            override fun add(element: T): Unit = throw UnsupportedOperationException()
            override fun remove(): Unit = throw UnsupportedOperationException()
            override fun set(element: T): Unit = throw UnsupportedOperationException()
        }
    }

    override fun removeAt(index: Int): T {
        return synchronized(locker) {
            getOrCloneListNoLock().removeAt(index)
        }
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> {
        return synchronized(locker) {
            list.subList(fromIndex, toIndex)
        }
    }

    override fun set(index: Int, element: T): T {
        return synchronized(locker) {
            getOrCloneListNoLock().set(index, element)
        }
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        return synchronized(locker) {
            list.retainAll(elements)
        }
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        return synchronized(locker) {
            getOrCloneListNoLock().removeAll(elements)
        }
    }

    override fun remove(element: T): Boolean {
        return synchronized(locker) {
            getOrCloneListNoLock().remove(element)
        }
    }

    override fun lastIndexOf(element: T): Int {
        return synchronized(locker) {
            list.lastIndexOf(element)
        }
    }

    override fun indexOf(element: T): Int {
        return synchronized(locker) {
            list.indexOf(element)
        }
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        return synchronized(locker) {
            list.containsAll(elements)
        }
    }

    override fun contains(element: T): Boolean {
        return synchronized(locker) {
            list.contains(element)
        }
    }

    override fun removeIf(filter: Predicate<in T>): Boolean {
        var removed: Boolean

        updateState { snapshot ->
            removed = false

            mutableListOf<T>().also { newList ->
                snapshot.forEach { item ->
                    if (filter.test(item)) {
                        removed = true
                    } else {
                        newList.add(item)
                    }
                }
            }
        }

        return removed
    }

    override fun replaceAll(operator: UnaryOperator<T>) {
        updateState { snapshot ->
            snapshot.asSequence().map { operator.apply(it) }.toMutableList()
        }
    }

    override fun sort(c: Comparator<in T>) {
        updateState { snapshot ->
            snapshot.toMutableList().apply {
                sortWith(c)
            }
        }
    }

    @OptIn(ExperimentalContracts::class)
    private fun updateState(getNewList: (List<T>) -> MutableList<T>) {
        contract {
            callsInPlace(getNewList, InvocationKind.AT_LEAST_ONCE)
        }

        while (true) {
            val localList = synchronized(locker) {
                isUnderReadingCount++
                list
            }

            try {
                val newList = getNewList(localList)

                synchronized(locker) {
                    if (localList == list) {

                        list = newList
                        assert(isUnderReadingCount > 0)
                        isUnderReadingCount = 0

                        return
                    }
                }

            } catch (e: Throwable) {

                if (localList == list) {
                    synchronized(locker) {
                        if (localList == list) {
                            val count = isUnderReadingCount--
                            assert(count >= 0)
                        }
                    }
                }

                throw e
            }
        }
    }


    private inline fun underReading(action: (List<T>) -> Unit) {
        val localList = synchronized(locker) {
            isUnderReadingCount++
            list
        }

        try {
            action(localList)
        } finally {

            synchronized(locker) {
                if (localList == list) {
                    val count = isUnderReadingCount--
                    assert(count >= 0)
                }
            }
        }
    }

    private fun getOrCloneListNoLock(): MutableList<T> {
        var localList = list
        if (isUnderReadingCount > 0) {
            localList = ArrayList(localList)
            isUnderReadingCount = 0
            list = localList
            return localList
        }

        return localList
    }
}