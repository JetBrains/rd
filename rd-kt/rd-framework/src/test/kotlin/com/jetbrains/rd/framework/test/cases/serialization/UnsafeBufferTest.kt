package com.jetbrains.rd.framework.test.cases.serialization

import com.jetbrains.rd.framework.UnsafeBuffer
import com.jetbrains.rd.framework.test.cases.A
import com.jetbrains.rd.framework.test.cases.B
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class UnsafeBufferTest {
    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testIntArrayWrite(backedByByteArray: Boolean) {
        val buf = if (backedByByteArray) UnsafeBuffer(ByteArray(0)) else UnsafeBuffer(0)

        val array = IntArray(100) { it }

        buf.writeIntArray(array)

        buf.rewind()

        val readArray = buf.readIntArray()
        assert(array.contentEquals(readArray))
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testIntArrayOverflowReadEmpty(backedByByteArray: Boolean) {
        Assertions.assertThrows(IndexOutOfBoundsException::class.java) {
            val buf = if (backedByByteArray) UnsafeBuffer(ByteArray(0)) else UnsafeBuffer(0)

            buf.readIntArray()
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testIntArrayOverflowReadWrongSize(backedByByteArray: Boolean) {
        Assertions.assertThrows(IndexOutOfBoundsException::class.java) {

            val buf = if (backedByByteArray) UnsafeBuffer(ByteArray(0)) else UnsafeBuffer(0)

            buf.writeInt(100)

            buf.rewind()

            buf.readIntArray()
        }
    }

    // todo: tests to cover other aspects of the buffer

    @ExperimentalUnsignedTypes
    @Test
    fun testUnsignedTypes() {
        val buf = UnsafeBuffer(100)

        buf.writeUShort(UShort.MIN_VALUE)
        buf.writeUShort(UShort.MAX_VALUE)

        buf.writeUInt(UInt.MIN_VALUE)
        buf.writeUInt(UInt.MAX_VALUE)

        buf.writeULong(ULong.MIN_VALUE)
        buf.writeULong(ULong.MAX_VALUE)

        assertEquals(buf.position, (UShort.SIZE_BYTES + UInt.SIZE_BYTES + ULong.SIZE_BYTES) * 2)

        buf.rewind()

        assertEquals(UShort.MIN_VALUE, buf.readUShort())
        assertEquals(UShort.MAX_VALUE, buf.readUShort())

        assertEquals(UInt.MIN_VALUE, buf.readUInt())
        assertEquals(UInt.MAX_VALUE, buf.readUInt())

        assertEquals(ULong.MIN_VALUE, buf.readULong())
        assertEquals(ULong.MAX_VALUE, buf.readULong())
    }

    @ExperimentalUnsignedTypes
    @Test
    fun testUnsignedArray() {
        val buf = UnsafeBuffer(100)

        val array = UIntArray(10) { UInt.MAX_VALUE }

        buf.writeUIntArray(array)

        assertEquals(Int.SIZE_BYTES + 10 * UInt.SIZE_BYTES, buf.position)

        buf.rewind()

        assertEquals(array.toString(), buf.readUIntArray().toString())
    }

    @Test
    fun testShrinking1() {
        val initialSize = 2 * 1024 * 1024
        val buf = UnsafeBuffer(initialSize.toLong())

        buf.rewind()
        assertEquals(initialSize, buf.allocated)

        buf.reset()
        assertEquals(1024 * 1024, buf.allocated)

        buf.reset()
        assertEquals(1024 * 1024, buf.allocated)
    }

    @Test
    fun testShrinking2() {
        val initialSize = 2 * 1024 * 1024
        val buf = UnsafeBuffer(initialSize.toLong())

        buf.writeByteArray(ByteArray(initialSize - 16))
        assertEquals(initialSize, buf.allocated)

        buf.reset()
        assertEquals(1024 * 1024, buf.allocated)
    }

    @Test
    fun testShrinking3() {
        val initialSize = 65536
        val buf = UnsafeBuffer(initialSize.toLong())

        buf.writeByteArray(ByteArray(2 * 1024 * 1024))
        assert(buf.allocated >= 2 * 1024 * 1024)

        buf.reset()
        assertEquals(1024 * 1024, buf.allocated)
    }


    @Test
    fun testInheritance() {
        val b = B("a")
        val x = (b as A).a
        assertEquals("a", x)
    }
}