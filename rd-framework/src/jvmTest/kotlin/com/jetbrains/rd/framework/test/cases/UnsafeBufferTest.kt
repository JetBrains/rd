package com.jetbrains.rd.framework.test.cases

import com.jetbrains.rd.framework.UnsafeBuffer
import org.junit.Test
import org.junit.experimental.theories.DataPoint
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(Theories::class)
class UnsafeBufferTest {

    companion object {
        @DataPoint
        @JvmField
        val trueValue = true

        @DataPoint
        @JvmField
        val falseValue = false
    }

    @Theory
    fun testIntArrayWrite(backedByByteArray: Boolean) {
        val buf = if (backedByByteArray) UnsafeBuffer(ByteArray(0)) else UnsafeBuffer(0)

        val array = IntArray(100) { it }

        buf.writeIntArray(array)

        buf.rewind()

        val readArray = buf.readIntArray()
        assert(array.contentEquals(readArray))
    }

    @Theory
    @Test(expected = IndexOutOfBoundsException::class)
    fun testIntArrayOverflowReadEmpty(backedByByteArray: Boolean) {
        val buf = if (backedByByteArray) UnsafeBuffer(ByteArray(0)) else UnsafeBuffer(0)

        buf.readIntArray()
    }

    @Theory
    @Test(expected = IndexOutOfBoundsException::class)
    fun testIntArrayOverflowReadWrongSize(backedByByteArray: Boolean) {
        val buf = if (backedByByteArray) UnsafeBuffer(ByteArray(0)) else UnsafeBuffer(0)

        buf.writeInt(100)

        buf.rewind()

        buf.readIntArray()
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
}