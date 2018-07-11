package com.jetbrains.rider.framework.impl

data class Range(val start: Int, val end: Int) {
    companion object {
        val invalidRange = Range(-1, -1)
    }

    val isNormalized: Boolean = this.start <= this.end

    fun shift(offset: Int) = Range(this.start + offset, this.end + offset)
}

fun Range.isEmpty(): Boolean = start == end
val Range.length get() = end - start