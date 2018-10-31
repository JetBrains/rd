package com.jetbrains.rider.framework.test.util

import com.jetbrains.rider.framework.UnsafeBuffer
import com.jetbrains.rider.framework.readArray
import com.jetbrains.rider.framework.writeArray
import org.junit.Test

class SerializersTest {



    @Test
    fun testReadArray() {
        val buffer = UnsafeBuffer(ByteArray(10))
        buffer.writeArray(Array(1) {"abc"}, {});
        buffer.rewind()

        val arr = buffer.readArray { "abc" }
    }

}