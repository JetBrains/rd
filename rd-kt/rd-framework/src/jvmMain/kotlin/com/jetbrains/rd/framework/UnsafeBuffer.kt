package com.jetbrains.rd.framework

import sun.misc.Unsafe
import java.io.Closeable
import java.util.*

class UnsafeBuffer private constructor(): AbstractBuffer(), Closeable {
    private var byteBufferMemoryBase: ByteArray? = null
    private var memory: Long = 0
    private var offset: Long = 0
    private var size: Long = 0
    private val unsafe = Companion.unsafe // copy to instance field for performance
    private var charArray: CharArray = charArrayOf() // preallocated array for string serialization

    override var position: Int
        get() = offset.toInt()
        set(value) { offset = value.toLong() }

    val allocated: Int
        get() = size.toInt()

    constructor(initialSize: Long): this() {
        memory = unsafe.allocateMemory(initialSize)
        byteBufferMemoryBase = null
        size = initialSize
    }

    constructor(byteArray: ByteArray): this() {
        byteBufferMemoryBase = byteArray
        memory = Unsafe.ARRAY_BYTE_BASE_OFFSET.toLong()
        size = byteArray.size.toLong()
    }

    override fun checkAvailable(moreSize: Int) {
        if (offset + moreSize > size)
            throw IndexOutOfBoundsException("Expected $moreSize bytes in buffer, only ${size - offset} available")
    }

    override fun getArray(): ByteArray {
        return byteBufferMemoryBase?: error("This unsafe buffer is on top of unsafe memory")
    }

    private fun requireAvailable(moreSize: Long) {
        if (offset + moreSize > size) {
            val newSize = Math.max(size * 2, offset + moreSize)
            reallocateMemory(newSize)
        }
    }

    private fun reallocateMemory(newSize: Long) {
        if (byteBufferMemoryBase == null) {
            memory = unsafe.reallocateMemory(memory, newSize)
        } else {
            byteBufferMemoryBase = Arrays.copyOf(byteBufferMemoryBase, newSize.toInt())
        }
        size = newSize
    }

    @Suppress("unused")
    inline private fun <reified T> read(size: Long, reader: (Any?, Long) -> T) : T {
        checkAvailable(size.toInt())

        val result = reader(byteBufferMemoryBase, memory + offset)
        offset += size
        return result
    }

    @Suppress("unused") // See bottom of the file for this and the other unused inline methods
    inline private fun <reified T> write(size: Long, writer: (Any?, Long, T) -> Unit, value: T) {
        requireAvailable(size)

        writer(byteBufferMemoryBase, memory + offset, value)
        offset += size
    }

    @Suppress("unused")
    inline private fun <reified T> readArray(arrayBase: Int, arrayStride: Int, arrayCtor: (Int) -> T): T {
        val len = readInt()

        checkAvailable(len * arrayStride)

        val arr = arrayCtor(len)
        unsafe.copyMemory(byteBufferMemoryBase, memory + offset, arr, arrayBase.toLong(), len * arrayStride.toLong())
        offset += len * arrayStride.toLong()
        return arr
    }

    @Suppress("unused", "NOTHING_TO_INLINE")
    private inline fun writeArray(arrayBase: Int, arrayStride: Int, array: Any, len: Int) {
        writeInt(len)

        requireAvailable(len * arrayStride.toLong())

        unsafe.copyMemory(array, arrayBase.toLong(), byteBufferMemoryBase, memory + offset, len * arrayStride.toLong())
        offset += len * arrayStride.toLong()
    }



    override fun readByteArray(): ByteArray {
        val length = readInt()

        val storage = ByteArray(length)
        checkAvailable(length)
        unsafe.copyMemory(byteBufferMemoryBase, memory + offset, storage, Unsafe.ARRAY_BYTE_BASE_OFFSET.toLong(), length.toLong())
        offset += length
        return storage
    }

    override fun readByteArrayRaw(array: ByteArray) {
        checkAvailable(array.size)
        unsafe.copyMemory(byteBufferMemoryBase, memory + offset, array, Unsafe.ARRAY_BYTE_BASE_OFFSET.toLong(), array.size.toLong())
        offset += array.size
    }

    override fun writeByteArray(array: ByteArray) {
        requireAvailable(4L + array.size)

        unsafe.putInt(byteBufferMemoryBase,memory + offset, array.size)
        unsafe.copyMemory(array, Unsafe.ARRAY_BYTE_BASE_OFFSET.toLong(), byteBufferMemoryBase, offset + memory + 4, array.size.toLong())

        offset += 4 + array.size
    }

    override fun writeByteArrayRaw(array: ByteArray, count: Int?) {
        val sz = count ?: (array.size - 0)
        require(sz >= 0 && sz <= array.size) { "sz >= 0 && sz <= array.size, sz = $sz, array.size=${array.size}" }

        requireAvailable(sz. toLong())

        unsafe.copyMemory(array, Unsafe.ARRAY_BYTE_BASE_OFFSET.toLong(), byteBufferMemoryBase, offset + memory, sz.toLong())

        offset += sz
    }

    override fun readNullableString(): String? {
        checkAvailable(4)

        val len = unsafe.getInt(byteBufferMemoryBase,memory + offset)
        offset += 4

        if (len < 0) {
            return null
        }

        checkAvailable(len * 2)

        if (charArray.size < len)
            charArray = CharArray(len * 2)

        val chars = charArray

        unsafe.copyMemory(byteBufferMemoryBase, memory + offset, chars, Unsafe.ARRAY_CHAR_BASE_OFFSET.toLong(), len * 2L)
        offset += len * 2

        return String(chars, 0, len)
    }

    override fun writeNullableString(value: String?) {
        if (value == null) {
            writeInt(-1)
            return
        }
        val len = value.length

        requireAvailable(4 + len * 2L)

        unsafe.putInt(byteBufferMemoryBase,memory + offset, len)
        offset += 4

        if (charArray.size < len)
            charArray = CharArray(len * 2)

        val chars = charArray
        value.toCharArray(chars)

        unsafe.copyMemory(chars, Unsafe.ARRAY_CHAR_BASE_OFFSET.toLong(), byteBufferMemoryBase, memory + offset, len * 2L)
        offset += len * 2
    }



    override fun reset() {
        offset = 0
        if (size > maximumSizeBeforeShrink) {
            reallocateMemory(maximumSizeBeforeShrink.toLong())
        }
    }

    override fun close() {
        if (byteBufferMemoryBase == null)
            unsafe.freeMemory(memory)
        else
            byteBufferMemoryBase = null
        memory = 0
    }

    @Suppress("unused", "ProtectedInFinal")
    protected fun finalize() {
        close()
    }

    companion object {
        private val unsafe: Unsafe
        private const val maximumSizeBeforeShrink = 1024 * 1024 // 1M

        init {
            val field = Unsafe::class.java.getDeclaredField("theUnsafe")
            field.isAccessible = true
            unsafe = field.get(null) as Unsafe
        }
    }

    override fun writeByte(value: Byte) {
        requireAvailable(1)
        unsafe.putByte(byteBufferMemoryBase, memory + offset, value)
        offset += 1
    }
    override fun readByte(): Byte {
        checkAvailable(1)
        val result = unsafe.getByte(byteBufferMemoryBase, memory + offset)
        offset += 1
        return result
    }

    override fun writeShort(value: Short) {
        requireAvailable(2)
        unsafe.putShort(byteBufferMemoryBase, memory + offset, value)
        offset += 2
    }
    override fun readShort(): Short {
        checkAvailable(2)
        val result = unsafe.getShort(byteBufferMemoryBase, memory + offset)
        offset += 2
        return result
    }

    override fun writeInt(value: Int) {
        requireAvailable(4)
        unsafe.putInt(byteBufferMemoryBase, memory + offset, value)
        offset += 4
    }
    override fun readInt(): Int {
        checkAvailable(4)
        val result = unsafe.getInt(byteBufferMemoryBase, memory + offset)
        offset += 4
        return result
    }

    override fun writeLong(value: Long) {
        requireAvailable(8)
        unsafe.putLong(byteBufferMemoryBase, memory + offset, value)
        offset += 8
    }
    override fun readLong(): Long {
        checkAvailable(8)
        val result = unsafe.getLong(byteBufferMemoryBase, memory + offset)
        offset += 8
        return result
    }

    override fun writeFloat(value: Float) {
        requireAvailable(4)
        unsafe.putFloat(byteBufferMemoryBase, memory + offset, value)
        offset += 4
    }
    override fun readFloat(): Float {
        checkAvailable(4)
        val result = unsafe.getFloat(byteBufferMemoryBase, memory + offset)
        offset += 4
        return result
    }

    override fun writeDouble(value: Double) {
        requireAvailable(8)
        unsafe.putDouble(byteBufferMemoryBase, memory + offset, value)
        offset += 8
    }
    override fun readDouble(): Double {
        checkAvailable(8)
        val result = unsafe.getDouble(byteBufferMemoryBase, memory + offset)
        offset += 8
        return result
    }

    override fun writeBoolean(value: Boolean) = writeByte(if(value) 1 else 0)
    override fun readBoolean() = readByte() != 0.toByte()

    override fun writeChar(value: Char) {
        requireAvailable(2)
        unsafe.putChar(byteBufferMemoryBase, memory + offset, value)
        offset += 2
    }
    override fun readChar(): Char {
        checkAvailable(2)
        val result = unsafe.getChar(byteBufferMemoryBase, memory + offset)
        offset += 2
        return result
    }

    override fun readCharArray(): CharArray {
        val len = readInt()
        checkAvailable(len * Unsafe.ARRAY_CHAR_INDEX_SCALE)
        val arr = CharArray(len)
        unsafe.copyMemory(byteBufferMemoryBase, memory + offset, arr, Unsafe.ARRAY_CHAR_BASE_OFFSET.toLong(), len * Unsafe.ARRAY_CHAR_INDEX_SCALE.toLong())
        offset += len * Unsafe.ARRAY_CHAR_INDEX_SCALE.toLong()
        return arr
    }
    override fun writeCharArray(array: CharArray) {
        writeInt(array.size)
        requireAvailable(array.size * Unsafe.ARRAY_CHAR_INDEX_SCALE.toLong())
        unsafe.copyMemory(array, Unsafe.ARRAY_CHAR_BASE_OFFSET.toLong(), byteBufferMemoryBase, memory + offset, array.size * Unsafe.ARRAY_CHAR_INDEX_SCALE.toLong())
        offset += array.size * Unsafe.ARRAY_CHAR_INDEX_SCALE.toLong()
    }

    override fun readShortArray(): ShortArray {
        val len = readInt()
        checkAvailable(len * Unsafe.ARRAY_SHORT_INDEX_SCALE)
        val arr = ShortArray(len)
        unsafe.copyMemory(byteBufferMemoryBase, memory + offset, arr, Unsafe.ARRAY_SHORT_BASE_OFFSET.toLong(), len * Unsafe.ARRAY_SHORT_INDEX_SCALE.toLong())
        offset += len * Unsafe.ARRAY_SHORT_INDEX_SCALE.toLong()
        return arr
    }
    override fun writeShortArray(array: ShortArray) {
        writeInt(array.size)
        requireAvailable(array.size * Unsafe.ARRAY_SHORT_INDEX_SCALE.toLong())
        unsafe.copyMemory(array, Unsafe.ARRAY_SHORT_BASE_OFFSET.toLong(), byteBufferMemoryBase, memory + offset, array.size * Unsafe.ARRAY_SHORT_INDEX_SCALE.toLong())
        offset += array.size * Unsafe.ARRAY_SHORT_INDEX_SCALE.toLong()
    }

    override fun readIntArray(): IntArray {
        val len = readInt()
        checkAvailable(len * Unsafe.ARRAY_INT_INDEX_SCALE)
        val arr = IntArray(len)
        unsafe.copyMemory(byteBufferMemoryBase, memory + offset, arr, Unsafe.ARRAY_INT_BASE_OFFSET.toLong(), len * Unsafe.ARRAY_INT_INDEX_SCALE.toLong())
        offset += len * Unsafe.ARRAY_INT_INDEX_SCALE.toLong()
        return arr
    }
    override fun writeIntArray(array: IntArray) {
        writeInt(array.size)
        requireAvailable(array.size * Unsafe.ARRAY_INT_INDEX_SCALE.toLong())
        unsafe.copyMemory(array, Unsafe.ARRAY_INT_BASE_OFFSET.toLong(), byteBufferMemoryBase, memory + offset, array.size * Unsafe.ARRAY_INT_INDEX_SCALE.toLong())
        offset += array.size * Unsafe.ARRAY_INT_INDEX_SCALE.toLong()
    }

    override fun readLongArray(): LongArray {
        val len = readInt()
        checkAvailable(len * Unsafe.ARRAY_LONG_INDEX_SCALE)
        val arr = LongArray(len)
        unsafe.copyMemory(byteBufferMemoryBase, memory + offset, arr, Unsafe.ARRAY_LONG_BASE_OFFSET.toLong(), len * Unsafe.ARRAY_LONG_INDEX_SCALE.toLong())
        offset += len * Unsafe.ARRAY_LONG_INDEX_SCALE.toLong()
        return arr
    }
    override fun writeLongArray(array: LongArray) {
        writeInt(array.size)
        requireAvailable(array.size * Unsafe.ARRAY_LONG_INDEX_SCALE.toLong())
        unsafe.copyMemory(array, Unsafe.ARRAY_LONG_BASE_OFFSET.toLong(), byteBufferMemoryBase, memory + offset, array.size * Unsafe.ARRAY_LONG_INDEX_SCALE.toLong())
        offset += array.size * Unsafe.ARRAY_LONG_INDEX_SCALE.toLong()
    }

    override fun readFloatArray(): FloatArray {
        val len = readInt()
        checkAvailable(len * Unsafe.ARRAY_FLOAT_INDEX_SCALE)
        val arr = FloatArray(len)
        unsafe.copyMemory(byteBufferMemoryBase, memory + offset, arr, Unsafe.ARRAY_FLOAT_BASE_OFFSET.toLong(), len * Unsafe.ARRAY_FLOAT_INDEX_SCALE.toLong())
        offset += len * Unsafe.ARRAY_FLOAT_INDEX_SCALE.toLong()
        return arr
    }
    override fun writeFloatArray(array: FloatArray) {
        writeInt(array.size)
        requireAvailable(array.size * Unsafe.ARRAY_FLOAT_INDEX_SCALE.toLong())
        unsafe.copyMemory(array, Unsafe.ARRAY_FLOAT_BASE_OFFSET.toLong(), byteBufferMemoryBase, memory + offset, array.size * Unsafe.ARRAY_FLOAT_INDEX_SCALE.toLong())
        offset += array.size * Unsafe.ARRAY_FLOAT_INDEX_SCALE.toLong()
    }

    override fun readDoubleArray(): DoubleArray {
        val len = readInt()
        checkAvailable(len * Unsafe.ARRAY_DOUBLE_INDEX_SCALE)
        val arr = DoubleArray(len)
        unsafe.copyMemory(byteBufferMemoryBase, memory + offset, arr, Unsafe.ARRAY_DOUBLE_BASE_OFFSET.toLong(), len * Unsafe.ARRAY_DOUBLE_INDEX_SCALE.toLong())
        offset += len * Unsafe.ARRAY_DOUBLE_INDEX_SCALE.toLong()
        return arr
    }
    override fun writeDoubleArray(array: DoubleArray) {
        writeInt(array.size)
        requireAvailable(array.size * Unsafe.ARRAY_DOUBLE_INDEX_SCALE.toLong())
        unsafe.copyMemory(array, Unsafe.ARRAY_DOUBLE_BASE_OFFSET.toLong(), byteBufferMemoryBase, memory + offset, array.size * Unsafe.ARRAY_DOUBLE_INDEX_SCALE.toLong())
        offset += array.size * Unsafe.ARRAY_DOUBLE_INDEX_SCALE.toLong()
    }

    override fun readBooleanArray(): BooleanArray {
        val len = readInt()
        checkAvailable(len * Unsafe.ARRAY_BOOLEAN_INDEX_SCALE)
        val arr = BooleanArray(len)
        unsafe.copyMemory(byteBufferMemoryBase, memory + offset, arr, Unsafe.ARRAY_BOOLEAN_BASE_OFFSET.toLong(), len * Unsafe.ARRAY_BOOLEAN_INDEX_SCALE.toLong())
        offset += len * Unsafe.ARRAY_BOOLEAN_INDEX_SCALE.toLong()
        return arr
    }
    override fun writeBooleanArray(array: BooleanArray) {
        writeInt(array.size)
        requireAvailable(array.size * Unsafe.ARRAY_BOOLEAN_INDEX_SCALE.toLong())
        unsafe.copyMemory(array, Unsafe.ARRAY_BOOLEAN_BASE_OFFSET.toLong(), byteBufferMemoryBase, memory + offset, array.size * Unsafe.ARRAY_BOOLEAN_INDEX_SCALE.toLong())
        offset += array.size * Unsafe.ARRAY_BOOLEAN_INDEX_SCALE.toLong()
    }

    // So, Kotlin 1.1 has some issues with inlining, including performance and correctness.
    // Therefore, all (read|write)*[Array] methods will be hand-inlined.
    // However, to preserve the ability to easily modify them, they (and their source non-inlined methods) will be put in a comment here.

    /*
    override fun writeByte(value: Byte) = write(1, unsafe::putByte, value)
    override fun readByte() = read(1, unsafe::getByte)

    override fun writeShort(value: Short) = write(2, unsafe::putShort, value)
    override fun readShort() = read(2, unsafe::getShort)

    override fun writeInt(value: Int) = write(4, unsafe::putInt, value)
    override fun readInt() = read(4, unsafe::getInt)

    override fun writeLong(value: Long) = write(8, unsafe::putLong, value)
    override fun readLong() = read(8, unsafe::getLong)

    override fun writeFloat(value: Float) = write(4, unsafe::putFloat, value)
    override fun readFloat() = read(4, unsafe::getFloat)

    override fun writeDouble(value: Double) = write(8, unsafe::putDouble, value)
    override fun readDouble() = read(8, unsafe::getDouble)

    override fun writeBoolean(value: Boolean) = writeByte(if(value) 1 else 0)
    override fun readBoolean() = readByte() != 0.toByte()

    override fun writeChar(value: Char) = write(2, unsafe::putChar, value)
    override fun readChar() = read(2, unsafe::getChar)


    override fun readCharArray() = readArray(Unsafe.ARRAY_CHAR_BASE_OFFSET, Unsafe.ARRAY_CHAR_INDEX_SCALE, ::CharArray)
    override fun writeCharArray(array: CharArray) = writeArray(Unsafe.ARRAY_CHAR_BASE_OFFSET, Unsafe.ARRAY_CHAR_INDEX_SCALE, array, array.size)

    override fun readShortArray() = readArray(Unsafe.ARRAY_SHORT_BASE_OFFSET, Unsafe.ARRAY_SHORT_INDEX_SCALE, ::ShortArray)
    override fun writeShortArray(array: ShortArray) = writeArray(Unsafe.ARRAY_SHORT_BASE_OFFSET, Unsafe.ARRAY_SHORT_INDEX_SCALE, array, array.size)

    override fun readIntArray() = readArray(Unsafe.ARRAY_INT_BASE_OFFSET, Unsafe.ARRAY_INT_INDEX_SCALE, ::IntArray)
    override fun writeIntArray(array: IntArray) = writeArray(Unsafe.ARRAY_INT_BASE_OFFSET, Unsafe.ARRAY_INT_INDEX_SCALE, array, array.size)

    override fun readLongArray() = readArray(Unsafe.ARRAY_LONG_BASE_OFFSET, Unsafe.ARRAY_LONG_INDEX_SCALE, ::LongArray)
    override fun writeLongArray(array: LongArray) = writeArray(Unsafe.ARRAY_LONG_BASE_OFFSET, Unsafe.ARRAY_LONG_INDEX_SCALE, array, array.size)

    override fun readFloatArray() = readArray(Unsafe.ARRAY_FLOAT_BASE_OFFSET, Unsafe.ARRAY_FLOAT_INDEX_SCALE, ::FloatArray)
    override fun writeFloatArray(array: FloatArray) = writeArray(Unsafe.ARRAY_FLOAT_BASE_OFFSET, Unsafe.ARRAY_FLOAT_INDEX_SCALE, array, array.size)

    override fun readDoubleArray() = readArray(Unsafe.ARRAY_DOUBLE_BASE_OFFSET, Unsafe.ARRAY_DOUBLE_INDEX_SCALE, ::DoubleArray)
    override fun writeDoubleArray(array: DoubleArray) = writeArray(Unsafe.ARRAY_DOUBLE_BASE_OFFSET, Unsafe.ARRAY_DOUBLE_INDEX_SCALE, array, array.size)

    override fun readBooleanArray() = readArray(Unsafe.ARRAY_BOOLEAN_BASE_OFFSET, Unsafe.ARRAY_BOOLEAN_INDEX_SCALE, ::BooleanArray)
    override fun writeBooleanArray(array: BooleanArray) = writeArray(Unsafe.ARRAY_BOOLEAN_BASE_OFFSET, Unsafe.ARRAY_BOOLEAN_INDEX_SCALE, array, array.size)
     */
}
