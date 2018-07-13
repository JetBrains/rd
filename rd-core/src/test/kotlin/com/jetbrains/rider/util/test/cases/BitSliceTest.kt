package com.jetbrains.rider.util.test.cases

import com.jetbrains.rider.util.BitSlice
import com.jetbrains.rider.util.test.framework.RdTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class BitSliceTest : RdTestBase()  {

    private enum class Enum {
        Zero,
        One,
        Two,
        Three
    }

    private val sliceInt = BitSlice.int(4)
    private val sliceEnum = BitSlice.enum<Enum>(sliceInt)
    private val sliceBool = BitSlice.bool(sliceEnum)

    @Test
    fun testZero() {
        val x = 0
        assertEquals(0, sliceInt[x])
        assertEquals(Enum.Zero, sliceEnum[x])
        assertEquals(false, sliceBool[x])
    }

    @Test
    fun test15() {
        val x = 15
        assertEquals(x, sliceInt[x])
        assertEquals(Enum.Zero, sliceEnum[x])
        assertEquals(false, sliceBool[x])
    }

    @Test
    fun test16() {
        val x = 16
        assertEquals(0, sliceInt[x])
        assertEquals(Enum.One, sliceEnum[x])
        assertEquals(false, sliceBool[x])
    }

    @Test
    fun testUpdate() {
        var x = 0

        assertFails { sliceInt.updated(x, 16) }

        x = sliceInt.updated(x, 10)
        x = sliceEnum.updated(x, Enum.Two)
        x = sliceBool.updated(x, true)

        assertEquals(10, sliceInt[x])
        assertEquals(Enum.Two, sliceEnum[x])
        assertEquals(true, sliceBool[x])
    }
}