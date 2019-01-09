package com.jetbrains.rd.util.collections

class ImmutableStack<T : Any> private constructor(private val head: Node<T>?) : Iterable<T> {

    override fun iterator(): Iterator<T> {
        return object : Iterator<T> {
            private var current = head
            override fun hasNext(): Boolean {
                return current != null
            }

            override fun next(): T {
                val ret = current!!
                current = ret.next
                return ret.value
            }
        }
    }

    private data class Node<T>(val value: T, val next: Node<T>?)

    companion object {
        val empty = ImmutableStack<Any>(null)
        @Suppress("UNCHECKED_CAST")
        operator fun <T : Any> invoke(): ImmutableStack<T> = empty as ImmutableStack<T>
    }

    val isEmpty: Boolean get() = head == null

    fun push(value: T): ImmutableStack<T> {
        return ImmutableStack(Node(value, head))
    }

    fun pop(): Pair<ImmutableStack<T>, T>? {
        val (value, nxt) = head ?: return null
        return ImmutableStack(nxt) to value
    }

    fun peek(): T? = head?.value
}

fun <T : Any> ImmutableStack<T>.tail(): ImmutableStack<T> = pop()?.first ?: ImmutableStack()

fun <T : Any> List<T>.toImmutableStack() = foldRight(ImmutableStack<T>()) { v, acc -> acc.push(v) }