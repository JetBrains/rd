package com.jetbrains.rd.util

//Can't use ByteBuffer in multiplatform code

fun ByteArray.parseLong(offset: Int) : Long {
    require(offset + 8 <= size) {"Can't parse long: 8 bytes needed. Offset: $offset, size: $size"}

    var res = 0L
    for (i in offset..offset+7) {
        res = (res shl 8) or (this[i].toLong() and 0xff)
    }
    return res
}
fun ByteArray.putLong(long: Long, offset: Int)  {
    require(offset + 8 <= size) {"Can't put long: 8 bytes needed. Offset: $offset, size: $size"}

    for (i in 0..7) {
        this[offset+i] = (long ushr (8*(7-i))).toByte()
    }
}

//obvious way, better to use lookup table
fun log2ceil(value: Int) : Int {
    for (i in 0..30)
        if (1 shl i >= value)
            return i

    return 31
}
