package com.jetbrains.rd.framework.test.util

import com.jetbrains.rd.framework.UnsafeBuffer
import com.jetbrains.rd.framework.readArray
import com.jetbrains.rd.framework.writeArray
import com.jetbrains.rd.util.assert
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SerializersTest {



    @Test
    fun testReadArray() {
        val buffer = UnsafeBuffer(ByteArray(10))
        buffer.writeArray(Array(1) {"abc"}) {}
        buffer.rewind()

        assertTrue (arrayOf("abc") contentEquals buffer.readArray { "abc" })
    }

}