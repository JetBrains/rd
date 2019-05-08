package com.jetbrains.rd.util.collections

class CountingSet<T> private constructor(val map: MutableMap<T, Int>): Map<T, Int> by map {
    constructor() : this(mutableMapOf<T, Int>())

    /**
     * Returns new value
     */
    fun add(key: T, value: Int) : Int {
        val old = map[key] ?: 0
        val new = old + value

        if (new == 0)
            map.remove(key)
        else
            map[key] = new

        return new
    }

    override fun get(key: T): Int = map[key] ?: 0
}