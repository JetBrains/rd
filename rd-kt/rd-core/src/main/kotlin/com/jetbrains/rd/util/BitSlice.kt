package com.jetbrains.rd.util

open class BitSlice(val lowBit: Int, val bitCount: Int) {
    init {
        require(lowBit >= 0)  { "[lowBit] must be >= 0, actual: '$lowBit'"}
        require(bitCount > 0) { "[bitCount] must be > 0, actual '$bitCount'"}
    }

    val highBit: Int get() = lowBit + bitCount - 1
    private val mask : Int get() = (1 shl bitCount) - 1

    abstract class TypedBitSlice<T>(lowBit: Int, bitCount: Int) : BitSlice(lowBit, bitCount) {
        abstract operator fun get(host: Int) : T
        abstract fun updated(host: Int, value: T) : Int
    }

    private class IntBitSlice(lowBit: Int, bitCount: Int) : TypedBitSlice<Int>(lowBit, bitCount) {
        override fun get(host: Int) = getRaw(host)
        override fun updated(host: Int, value: Int) = updatedRaw(host, value)
    }

    companion object {
        fun int(bitCount: Int, prev: BitSlice? = null) : TypedBitSlice<Int> = IntBitSlice(prev.nextSliceLow(), bitCount)

        fun bool(prev: BitSlice? = null) : TypedBitSlice<Boolean> = object : TypedBitSlice<Boolean>(prev.nextSliceLow(), 1) {
            override fun get(host: Int) = getRaw(host) != 0
            override fun updated(host: Int, value: Boolean) = updatedRaw(host, if (value) 1 else 0)
        }

        inline fun <reified T:Enum<T>> enum(prev: BitSlice? = null) : TypedBitSlice<T> {
            val values = enumValues<T>()
            require(values.size >= 2) { "Bit slice for enums with $values values is meaningless" }
            return object : TypedBitSlice<T>(prev.nextSliceLow(), log2ceil(values.size)) {
                override fun get(host: Int): T = values[getRaw(host)]
                override fun updated(host: Int, value: T) = updatedRaw(host, value.ordinal)
            }
        }
    }


    private fun requireSliceFitsIntType() {
        val maxBit = 31
        require(highBit <= maxBit) { "$this doesn't fit into host type 'Int'; must be inside [0, $maxBit]"}
    }

    private fun requireValueFitsSlice(value: Int) {
        require(value >= 0) { "[value] must be >= 0; actual: '$value'" }
        require(value <= mask) {"[value] must be <= $mask to fit $this; actual '$value'"}
    }


    //GET and SET
    fun getRaw(host: Int) : Int {
        requireSliceFitsIntType()

        return (host ushr lowBit) and mask
    }


    fun updatedRaw(host: Int, value: Int) : Int {
        requireSliceFitsIntType()
        requireValueFitsSlice(value)

        return (host and  ((mask shl lowBit) xor -1)) or (value shl lowBit)
    }


    override fun toString(): String {
        return "BitSlice[$lowBit, $highBit]"
    }
}

fun BitSlice?.nextSliceLow() = this?.highBit?.plus(1) ?: 0

operator fun <T> BitSlice.TypedBitSlice<T>.get(host: AtomicInteger) = get(host.get())