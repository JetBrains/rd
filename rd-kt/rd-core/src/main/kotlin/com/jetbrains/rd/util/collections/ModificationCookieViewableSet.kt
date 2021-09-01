package com.jetbrains.rd.util.collections

import com.jetbrains.rd.util.reactive.IMutableViewableSet
import com.jetbrains.rd.util.reflection.usingValue
import kotlin.reflect.KMutableProperty0

class ModificationCookieViewableSet<E : Any>(private val backingSet: IMutableViewableSet<E>, val cookieProperty: KMutableProperty0<Boolean>) : IMutableViewableSet<E> by backingSet {
    override fun add(element: E): Boolean {
        return cookieProperty.usingValue(true) {
            backingSet.add(element)
        }
    }

    override fun clear() {
        cookieProperty.usingValue(true) {
            backingSet.clear()
        }
    }

    override fun remove(element: E): Boolean {
        return cookieProperty.usingValue(true) {
            backingSet.remove(element)
        }
    }

    override fun addAll(elements: Collection<E>): Boolean {
        return cookieProperty.usingValue(true) {
            backingSet.addAll(elements)
        }
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        return cookieProperty.usingValue(true) {
            backingSet.removeAll(elements)
        }
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        return cookieProperty.usingValue(true) {
            backingSet.retainAll(elements)
        }
    }

    override fun iterator(): MutableIterator<E> {
        val iterator = backingSet.iterator()
        return object : MutableIterator<E> {
            override fun hasNext() = iterator.hasNext()
            override fun next() = iterator.next()
            override fun remove() = cookieProperty.usingValue(true) { iterator.remove() }
        }
    }
}