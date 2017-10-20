package com.jetbrains.rider.util.reactive

import com.jetbrains.rider.util.lifetime.Lifetime
import java.util.*


class ViewableStack<T : Any>(vararg items: T): IViewableStack<T> {
    private val stack = ArrayDeque<T>()
    private val peekValue = Property<T?>()

    init {
        items.forEach { stack.push(it) }
    }

    override fun pop(): T {
        val old = stack.pop()

        val new = stack.peek()
        peekValue.value = new

        return old
    }

    override fun push(value: T) {
        if (value == stack.peek()) return

        stack.push(value)
        peekValue.value = value
    }

    override fun peek(): T = stack.peek()

    override fun view(lifetime: Lifetime, handler: (Lifetime, T) -> Unit) = peekValue.viewNotNull(lifetime, handler)

    override fun isEmpty(): Boolean = stack.isEmpty()

    override val size: Int get() = stack.size

    override fun contains(element: T): Boolean = stack.contains(element)

    override fun containsAll(elements: Collection<T>): Boolean = stack.containsAll(elements)

    override fun iterator(): Iterator<T> = stack.iterator()
}