package com.jetbrains.rider.util.test.cases

import com.jetbrains.rider.util.parseLong
import com.jetbrains.rider.util.putLong
import org.junit.Assert
import org.junit.Test
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

            Assert.assertArrayEquals(actual, expected)

            //parse
            Assert.assertEquals(actual.parseLong(0), l1)
            Assert.assertEquals(actual.parseLong(8), l2)
        }
    }
}