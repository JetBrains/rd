package com.jetbrains.rider.framework

abstract class AbstractBuffer {
    abstract var position: Int

    abstract fun writeByte(value: Byte)
    abstract fun readByte(): Byte

    abstract fun writeShort(value: Short)
    abstract fun readShort(): Short

    abstract fun writeInt(value: Int)
    abstract fun readInt(): Int

    abstract fun writeLong(value: Long)
    abstract fun readLong(): Long

    abstract fun writeFloat(value: Float)
    abstract fun readFloat(): Float

    abstract fun writeDouble(value: Double)
    abstract fun readDouble(): Double

    abstract fun writeBoolean(value: Boolean)
    abstract fun readBoolean(): Boolean

    abstract fun writeChar(value: Char)
    abstract fun readChar(): Char

    abstract fun readByteArray(): ByteArray
    abstract fun readByteArrayRaw(array: ByteArray)

    abstract fun writeByteArray(array: ByteArray)
    abstract fun writeByteArrayRaw(array: ByteArray)

    fun readString() = readNullableString()!!
    abstract fun readNullableString(): String?

    fun writeString(value: String) = writeNullableString(value)
    abstract fun writeNullableString(value: String?)
    
    abstract fun readCharArray(): CharArray
    abstract fun writeCharArray(array: CharArray)

    abstract fun readShortArray(): ShortArray
    abstract fun writeShortArray(array: ShortArray)

    abstract fun readIntArray(): IntArray
    abstract fun writeIntArray(array: IntArray)

    abstract fun readLongArray(): LongArray
    abstract fun writeLongArray(array: LongArray)

    abstract fun readFloatArray(): FloatArray
    abstract fun writeFloatArray(array: FloatArray)

    abstract fun readDoubleArray(): DoubleArray
    abstract fun writeDoubleArray(array: DoubleArray)

    abstract fun readBooleanArray(): BooleanArray
    abstract fun writeBooleanArray(array: BooleanArray)

    abstract fun rewind()

    abstract fun getArray() : ByteArray
}

expect fun createAbstractBuffer() : AbstractBuffer