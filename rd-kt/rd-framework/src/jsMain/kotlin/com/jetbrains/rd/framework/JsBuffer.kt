package com.jetbrains.rd.framework

import org.khronos.webgl.*

class JsBuffer(private var buffer: ArrayBuffer) : AbstractBuffer() {
    companion object {
        private val textDecoder = TextDecoder("utf-16le")
        private val textEncoder = TextEncoder("utf-16le", object {
            @Suppress("unused")
            val NONSTANDARD_allowLegacyEncoding = true
        })
        private const val maximumSizeBeforeShrink = 1024 * 1024 // 1M
    }

    private var dataView: DataView = DataView(buffer)
    private val littleEndian = true

    private var offset: Int = 0
    override var position: Int
        get() = offset
        set(value) {
            offset = value
        }

    override fun checkAvailable(moreSize: Int) {
        if (offset + moreSize > buffer.byteLength)
            throw IndexOutOfBoundsException()
    }

    private fun requireAvailable(bytesCount: Int) {
        val size = buffer.byteLength
        if (offset + bytesCount > size) {
            val newSize = maxOf(size * 2, offset + bytesCount)
            resizeBuffer(newSize)
        }
    }

    private fun resizeBuffer(newSize: Int) {
        val oldBuffer = buffer
        val oldBufferView = Uint8Array(oldBuffer)
        buffer = ArrayBuffer(newSize)
        Uint8Array(buffer).set(oldBufferView)
        dataView = DataView(buffer)
    }

    private inline fun <reified T> read(reader: (DataView, Int) -> T, sizeInBytes: Int): T {
        checkAvailable(sizeInBytes)
        val result = reader(dataView, position)
        offset += sizeInBytes
        return result
    }

    private inline fun <reified T> write(value: T, writer: (DataView, Int, T) -> Unit, sizeInBytes: Int) {
        requireAvailable(sizeInBytes)
        writer(dataView, position, value)
        offset += sizeInBytes
    }

    /// readArray cannot be expressed via readAndConvertArray because we can't put dynamic in generic type
    private inline fun <reified TResult, reified TView> readArray(createView: (ArrayBuffer, Int, Int) -> TView,
                                                                  createDest: (Int) -> TResult,
                                                                  elementSizeInBytes: Int): TResult {
        val length = readInt()
        return readArrayBody(length, createView, createDest, elementSizeInBytes)
    }

    private inline fun <reified TResult, reified TView> readArrayBody(length: Int,
                                                                      createView: (ArrayBuffer, Int, Int) -> TView,
                                                                      createDest: (Int) -> TResult,
                                                                      elementSizeInBytes: Int): TResult {
        val byteLength = length * elementSizeInBytes
        checkAvailable(byteLength)
        val arrayView = createView(buffer, position, length)
        val result = createDest(length)
        result.asDynamic().set(arrayView)
        position += byteLength
        return result
    }

    private inline fun <reified TResult> readAndConvertArray(createDest: (Int) -> TResult,
                                                             readAndConvert: (TResult, DataView, Int) -> Unit,
                                                             elementSizeInBytes: Int): TResult {
        val length = readInt()
        val byteLength = length * elementSizeInBytes
        checkAvailable(byteLength)
        val result = createDest(length)
        readAndConvert(result, dataView, position)
        position += byteLength
        return result
    }

    private inline fun <reified TResult, reified TView> writeArray(array: TResult, arraySize: Int,
                                                                   createView: (ArrayBuffer, Int, Int) -> TView,
                                                                   elementSizeInBytes: Int) {
        writeInt(arraySize)
        writeArrayBody(array, arraySize, createView, elementSizeInBytes)
    }

    private inline fun <reified TResult, reified TView> writeArrayBody(array: TResult, arraySize: Int,
                                                                       createView: (ArrayBuffer, Int, Int) -> TView,
                                                                       elementSizeInBytes: Int) {
        val byteLength = arraySize * elementSizeInBytes
        requireAvailable(byteLength)
        val arrayView = createView(buffer, position, arraySize)
        arrayView.asDynamic().set(array)
        position += byteLength
    }

    private inline fun <reified TResult> convertAndWriteArray(array: TResult, arraySize: Int,
                                                              convertAndWrite: (TResult, DataView, Int) -> Unit,
                                                              elementSizeInBytes: Int) {
        writeInt(arraySize)
        val byteLength = arraySize * elementSizeInBytes
        requireAvailable(byteLength)
        convertAndWrite(array, dataView, position)
        position += byteLength
    }

    override fun readIntArray() = readArray(
            { buffer, position, length -> Int32Array(buffer, position, length) },
            { length -> IntArray(length) },
            4
    )

    override fun writeIntArray(array: IntArray) = writeArray(
            array,
            array.size,
            { buffer, position, length -> Int32Array(buffer, position, length) },
            4
    )

    override fun writeByte(value: Byte) = write(value, { view, position, v -> view.setInt8(position, v) }, 1)

    override fun readByte() = read({ view, position -> view.getInt8(position) }, 1)

    override fun writeShort(
            value: Short) = write(value, { view, position, v -> view.setInt16(position, v, littleEndian) }, 2)

    override fun readShort() = read({ view, position -> view.getInt16(position, littleEndian) }, 2)

    override fun writeInt(
            value: Int) = write(value, { view, position, v -> view.setInt32(position, v, littleEndian) }, 4)

    override fun readInt() = read({ view, position -> view.getInt32(position, littleEndian) }, 4)

    override fun writeLong(
            value: Long) = write(value, { view, position, v -> view.setInt64(position, v, littleEndian) }, 8)

    override fun readLong() = read({ view, position -> view.getInt64(position, littleEndian) }, 8)

    override fun writeFloat(
            value: Float) = write(value, { view, position, v -> view.setFloat32(position, v, littleEndian) }, 4)

    override fun readFloat() = read({ view, position -> view.getFloat32(position, littleEndian) }, 4)

    override fun writeDouble(
            value: Double) = write(value, { view, position, v -> view.setFloat64(position, v, littleEndian) }, 8)

    override fun readDouble() = read({ view, position -> view.getFloat64(position, littleEndian) }, 8)

    override fun writeBoolean(value: Boolean) = writeByte(if (value) 1 else 0)

    override fun readBoolean() = readByte() != 0.toByte()

    override fun writeChar(value: Char) {
        requireAvailable(2)
        val encoded = textEncoder.encode(value.toString())
        writeArrayBody(encoded, encoded.length,
                { buffer, position, length -> Uint8Array(buffer, position, length) },
                1)
    }

    override fun readChar(): Char {
        checkAvailable(2)
        val decoded = textDecoder.decode(DataView(buffer, position, 2))
        position += 2
        return decoded[0]
    }

    override fun readByteArray() = readArray(
            { buffer, position, length -> Int8Array(buffer, position, length) },
            { length -> ByteArray(length) },
            1
    )

    override fun readByteArrayRaw(array: ByteArray) {
        readArrayBody(
                array.size,
                { buffer, position, length -> Int8Array(buffer, position, length) },
                { array },
                1)
    }

    override fun writeByteArray(array: ByteArray) = writeArray(
            array,
            array.size,
            { buffer, position, length -> Int8Array(buffer, position, length) },
            1
    )

    override fun writeByteArrayRaw(array: ByteArray, count: Int?) = writeArrayBody(array, count ?: array.size,
            { buffer, position, length -> Int8Array(buffer, position, length) }, 1)

    override fun readNullableString(): String? {
        val length = readInt()
        if (length < 0)
            return null
        val byteLength = length * 2
        val result = textDecoder.decode(DataView(buffer, position, byteLength))
        position += byteLength
        return result
    }


    override fun writeNullableString(value: String?) {
        if (value == null) {
            writeInt(-1)
            return
        }
        writeInt(value.length)
        val encoded = textEncoder.encode(value)
        writeArrayBody(encoded, encoded.length,
                { buffer, position, length -> Uint8Array(buffer, position, length) },
                1)
    }

    override fun readCharArray(): CharArray {
        val string = readNullableString()
        return CharArray(string!!.length) { string[it] }
    }

    override fun writeCharArray(array: CharArray) {
        val string = array.joinToString("")
        writeNullableString(string)
    }

    override fun readShortArray() = readArray(
            { buffer, position, length -> Int16Array(buffer, position, length) },
            { length -> ShortArray(length) },
            2
    )

    override fun writeShortArray(array: ShortArray) = writeArray(
            array,
            array.size,
            { buffer, position, length -> Int16Array(buffer, position, length) },
            2
    )

    override fun readLongArray() = readAndConvertArray(
            { length -> LongArray(length) },
            { dest, dataView, position ->
                for (i in 0 until dest.size) {
                    dest[i] = dataView.getInt64(position + i * 8, littleEndian)
                }
            },
            8
    )

    override fun writeLongArray(array: LongArray) = convertAndWriteArray(
            array,
            array.size,
            { source, dataView, position ->
                for (i in 0 until source.size) {
                    dataView.setInt64(position + i * 8, source[i], littleEndian)
                }
            },
            8
    )

    override fun readFloatArray() = readArray(
            { buffer, position, length -> Float32Array(buffer, position, length) },
            { length -> FloatArray(length) },
            4
    )

    override fun writeFloatArray(array: FloatArray) = writeArray(
            array,
            array.size,
            { buffer, position, length -> Float32Array(buffer, position, length) },
            4
    )

    override fun readDoubleArray() = readArray(
            { buffer, position, length -> Float64Array(buffer, position, length) },
            { length -> DoubleArray(length) },
            8
    )

    override fun writeDoubleArray(array: DoubleArray) = writeArray(
            array,
            array.size,
            { buffer, position, length -> Float64Array(buffer, position, length) },
            8
    )

    override fun readBooleanArray() = readAndConvertArray(
            { length -> BooleanArray(length) },
            { dest, dataView, position ->
                for (i in 0 until dest.size) {
                    dest[i] = dataView.getBoolean(position + i)
                }
            },
            1
    )

    override fun writeBooleanArray(array: BooleanArray) = convertAndWriteArray(
            array,
            array.size,
            { source, dataView, position ->
                for (i in 0 until source.size) {
                    dataView.setBoolean(position + i, source[i])
                }
            },
            1
    )

    override fun reset() {
        offset = 0
        if (buffer.byteLength > maximumSizeBeforeShrink) {
            resizeBuffer(maximumSizeBeforeShrink)
        }
    }

    fun getFirstBytes(length: Int): DataView {
        return DataView(buffer, 0, length)
    }

    override fun getArray(): ByteArray {
        TODO("not optimal, remove from interface") //To change body of created functions use File | Settings | File Templates.
    }

    //todo check correctness of default read/write of unsigned types
}