package com.jetbrains.rider.framework.test.util

import com.jetbrains.rider.framework.UnsafeBuffer
import com.jetbrains.rider.framework.readArray
import com.jetbrains.rider.framework.writeArray
import org.testng.annotations.Test

class SerializersTest {



    @Test
    fun testReadArray() {
        val buffer = UnsafeBuffer(ByteArray(10))
        buffer.writeArray(Array(1) {"abc"}, {});
        buffer.rewind()

        val arr = buffer.readArray { "abc" }
    }

    @Test
    fun testArrayDowncast() {
        val x = Array<Any?>(10) { null }
        val y = x as Array<String?>
        println(y)
    }
}