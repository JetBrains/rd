package com.jetbrains.rider.util.collections

/**
 * Two stacks based queue
 */
class QueueImpl<T>() {
    private var toPush = ArrayList<T?>()
    private var toPoll = ArrayList<T?>()
    var pollIndex = 0

    fun offer(v: T) {
        toPush.add(v)
    }

    fun poll() : T? {
        if (isEmpty()) return null

        if(pollIndex >= toPoll.size)
            return null

        val res = toPoll[pollIndex]
        toPoll[pollIndex++] = null
        return res
    }

    fun peek() : T? {
        if (isEmpty()) return null

        return toPoll[pollIndex]
    }

    fun isEmpty(): Boolean {
        if (pollIndex < toPoll.size) return false
        if (toPush.isEmpty()) return true

        toPoll = toPush
        toPush = ArrayList<T?>()
        pollIndex = 0

        return false
    }

    fun clear() {
        while (!isEmpty()) poll()
    }

}