package com.jetbrains.rd.util.test.cases

import com.jetbrains.rd.util.threading.ByteBufferAsyncProcessor
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

class ByteBufferAsyncProcessorTest {

    fun InputStream.readInt32() : Int? {
        val b1 = read()
        val b2 = read()
        val b3 = read()
        val b4 = read()
        val res = b1 or (b2 shl 8) or (b3 shl 16) or (b4 shl 24)
        if (res < 0)
            return null

        return res
    }

    fun Int.toByteArray() = byteArrayOf(
            (this ushr 0 and 0xff).toByte(),
            (this ushr 8 and 0xff).toByte(),
            (this ushr 16 and 0xff).toByte(),
            (this ushr 24 and 0xff).toByte()
            )

    @Test
    fun testOneProducer() {
        var prev = -1
        val buffer = ByteBufferAsyncProcessor("TestAsyncProcessor", 4) {
            assert(it.ptr > 0)
            val x = ByteArrayInputStream(it.data).readInt32()!!
            assert (x > prev)
            prev = x
        }

        buffer.start()
        repeat(1000) {
            buffer.put(it.toByteArray())
        }
    }
}