package com.jetbrains.rd.framework.impl

data class Range(val start: Int, val end: Int) {
    companion object {
        val invalidRange = Range(-1, -1)
    }

    val isNormalized: Boolean = this.start <= this.end

    fun shift(offset: Int) = Range(this.start + offset, this.end + offset)
}