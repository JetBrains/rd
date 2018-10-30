package com.jetbrains.rider.util.collections

/**
 * Two stacks based queue
 */
class QueueImpl<E>() {
    private var toPush = ArrayList<E?>()
    private var toPoll = ArrayList<E?>()
    var pollIndex = 0

    fun offer(element: E): Boolean {
        toPush.add(element)
        return true
    }

    fun poll() : E? {
        if (isEmpty()) return null

        if(pollIndex >= toPoll.size)
            return null

        val res = toPoll[pollIndex]
        toPoll[pollIndex++] = null
        return res
    }

    fun peek() : E? {
        if (isEmpty()) return null

        return toPoll[pollIndex]
    }

    fun isEmpty(): Boolean {
        if (pollIndex < toPoll.size) return false
        if (toPush.isEmpty()) return true

        toPoll = toPush
        toPush = ArrayList<E?>()
        pollIndex = 0

        return false
    }

    fun clear() {
        while (!isEmpty()) poll()
    }

}