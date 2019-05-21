package com.jetbrains.rd.framework.test.util

import com.jetbrains.rd.framework.UnsafeBuffer
import com.jetbrains.rd.framework.readArray
import com.jetbrains.rd.framework.writeArray
import org.junit.Test

class SerializersTest {



    @Test
    fun testReadArray() {
        val buffer = UnsafeBuffer(ByteArray(10))
        buffer.writeArray(Array(1) {"abc"}) {}
        buffer.rewind()

        val arr = buffer.readArray { "abc" }
    }

}