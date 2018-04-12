package com.jetbrains.rider.framework

import org.khronos.webgl.DataView

fun DataView.getInt64(byteOffset: Int, littleEndian: Boolean = false) : Long {
    val high = this.getInt32(byteOffset + (if (littleEndian) 4 else 0), littleEndian)
    val low  = this.getInt32(byteOffset + (if (littleEndian) 0 else 4), littleEndian)
    return (high.toLong() shl 32) or (low.toLong() and 0xffffffffL)
}

fun DataView.setInt64(byteOffset: Int, value: Long, littleEndian: Boolean = false) {
    val high = (value ushr 32).toInt()
    val low = value.toInt()
    this.setInt32(byteOffset + (if (littleEndian) 4 else 0), high, littleEndian)
    this.setInt32(byteOffset + (if (littleEndian) 0 else 4), low,  littleEndian)
}

fun DataView.getBoolean(byteOffset: Int) : Boolean {
    val byte = this.getInt8(byteOffset)
    return !byte.equals(0)
}


fun DataView.setBoolean(byteOffset: Int, value: Boolean) {
    this.setInt8(byteOffset, if (value) 1 else 0)
}