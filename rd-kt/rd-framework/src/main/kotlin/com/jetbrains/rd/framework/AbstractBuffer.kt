package com.jetbrains.rd.framework

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.threading.SingleThreadScheduler

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
    abstract fun writeByteArrayRaw(array: ByteArray, count: Int? = null)

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

    abstract fun getArray(): ByteArray

    abstract fun checkAvailable(moreSize: Int)

    /**
     * Sets position to zero, can discard data in order to shrink backing storage
     */
    open fun reset() {
        position = 0
    }

    /**
     * Sets position to zero, keeps all data
     */
    fun rewind() {
        position = 0
    }

    //unsigned types
    @ExperimentalUnsignedTypes
    open fun readUByte() = readByte().toUByte()

    @ExperimentalUnsignedTypes
    open fun writeUByte(value: UByte) = writeByte(value.toByte())

    @ExperimentalUnsignedTypes
    open fun readUShort() = readShort().toUShort()

    @ExperimentalUnsignedTypes
    open fun writeUShort(value: UShort) = writeShort(value.toShort())

    @ExperimentalUnsignedTypes
    open fun readUInt(): UInt = readInt().toUInt()

    @ExperimentalUnsignedTypes
    open fun writeUInt(value: UInt) = writeInt(value.toInt())

    @ExperimentalUnsignedTypes
    open fun readULong(): ULong = readLong().toULong()

    @ExperimentalUnsignedTypes
    open fun writeULong(value: ULong) = writeLong(value.toLong())


    //unsigned arrays
    @ExperimentalUnsignedTypes
    open fun readUByteArray() : UByteArray = readByteArray().asUByteArray()

    @ExperimentalUnsignedTypes
    open fun writeUByteArray(array: UByteArray) = writeByteArray(array.asByteArray())
    
    
    @ExperimentalUnsignedTypes
    open fun readUShortArray() : UShortArray = readShortArray().asUShortArray()

    @ExperimentalUnsignedTypes
    open fun writeUShortArray(array: UShortArray) = writeShortArray(array.asShortArray())

    
    @ExperimentalUnsignedTypes
    open fun readUIntArray() : UIntArray = readIntArray().asUIntArray()

    @ExperimentalUnsignedTypes
    open fun writeUIntArray(array: UIntArray) = writeIntArray(array.asIntArray())

    
    @ExperimentalUnsignedTypes
    open fun readULongArray() : ULongArray = readLongArray().asULongArray()

    @ExperimentalUnsignedTypes
    open fun writeULongArray(array: ULongArray) = writeLongArray(array.asLongArray())
}

fun createAbstractBuffer(): AbstractBuffer {
    return UnsafeBuffer(ByteArray(12))
}

fun createBackgroundScheduler(lifetime: Lifetime, name: String) : IScheduler = SingleThreadScheduler(lifetime, name)

fun createAbstractBuffer(bytes: ByteArray): AbstractBuffer {
    return UnsafeBuffer(bytes)
}
