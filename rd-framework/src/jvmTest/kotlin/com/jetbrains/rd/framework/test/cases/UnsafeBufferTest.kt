package com.jetbrains.rd.framework.test.cases

import com.jetbrains.rd.framework.UnsafeBuffer
import org.junit.Test
import org.junit.experimental.theories.DataPoint
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith
import java.lang.IndexOutOfBoundsException

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
        val buf = if(backedByByteArray) UnsafeBuffer(ByteArray(0)) else UnsafeBuffer(0)

        val array = IntArray(100) { it }

        buf.writeIntArray(array)

        buf.rewind()

        val readArray = buf.readIntArray()
        assert(array.contentEquals(readArray))
    }

    @Theory
    @Test(expected = IndexOutOfBoundsException::class)
    fun testIntArrayOverflowReadEmpty(backedByByteArray: Boolean) {
        val buf = if(backedByByteArray) UnsafeBuffer(ByteArray(0)) else UnsafeBuffer(0)

        buf.readIntArray()
    }

    @Theory
    @Test(expected = IndexOutOfBoundsException::class)
    fun testIntArrayOverflowReadWrongSize(backedByByteArray: Boolean) {
        val buf = if(backedByByteArray) UnsafeBuffer(ByteArray(0)) else UnsafeBuffer(0)

        buf.writeInt(100)

        buf.rewind()

        buf.readIntArray()
    }

    // todo: tests to cover other aspects of the buffer
}