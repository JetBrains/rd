package com.jetbrains.rd.util.test.cases

import com.jetbrains.rd.util.log2ceil
import com.jetbrains.rd.util.parseLong
import com.jetbrains.rd.util.putLong
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

import java.nio.ByteBuffer
import java.util.*

class BitHacksTest {

    @Test
    fun testPutParseLong() {
        val expected = ByteArray(16)
        val actual = ByteArray(16)


        val rnd = Random()
        for (i in 0..100) {
            val (l1, l2) = rnd.nextLong() to rnd.nextLong()

            //put
            val bb = ByteBuffer.wrap(expected)
            bb.putLong(l1)
            bb.putLong(l2)

            actual.putLong(l1, 0)
            actual.putLong(l2, 8)

            assertArrayEquals(actual, expected)

            //parse
            assertEquals(actual.parseLong(0), l1)
            assertEquals(actual.parseLong(8), l2)
        }
    }

    @Test
    fun testLog2Ceil() {
        assertEquals(0, log2ceil(0))
        assertEquals(0, log2ceil(1))
        assertEquals(1, log2ceil(2))
        assertEquals(2, log2ceil(3))
        assertEquals(2, log2ceil(4))
        assertEquals(3, log2ceil(5))
    }
}