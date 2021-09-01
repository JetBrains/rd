package com.jetbrains.rd.util.collections

class ImmutableQueue<T : Any> private constructor(private val toPush: ImmutableStack<T>, private val toPoll: ImmutableStack<T>) {

    constructor() : this(ImmutableStack(), ImmutableStack())

    fun enqueue(value: T) = ImmutableQueue(toPush.push(value), toPoll)

    fun dequeue(): Pair<ImmutableQueue<T>, T>? {
        var (push, poll) = toPush to toPoll
        if (poll.isEmpty) {
            while (!push.isEmpty) {
                val v = push.peek()?:break
                push = push.tail()
                poll = poll.push(v)
            }
        }

        val v = poll.peek() ?: return null
        return ImmutableQueue(push, poll.tail()) to v
    }

}