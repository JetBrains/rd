package com.jetbrains.rd.framework.test.cases.serialization

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.util.string.RName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SerializersTest {

    @Test
    fun read_date_should_floor_negative_millis() {
        val buffer = UnsafeBuffer(ByteArray(100))
        buffer.writeLong(621355950301790001) // 1969-12-31 23:30:30.1790001 in ticks
        buffer.rewind()

        val dateTime = buffer.readDateTime()

        assertEquals (dateTime.time, -1769821) // 1969-12-31 23:30:30.179, NOT 1969-12-31 23:30:30.180
    }

    @Test
    fun testReadArray() {
        val buffer = UnsafeBuffer(ByteArray(10))
        buffer.writeArray(Array(1) {"abc"}) {}
        buffer.rewind()

        assertTrue (arrayOf("abc") contentEquals buffer.readArray { "abc" })
    }

    @Test
    fun testReadRName() {
        val testNames = listOf(
            RName.Empty,
            RName.Empty.sub("", ""),
            RName.Empty.sub("abc", ""),
            RName.Empty.sub("some very long string with numbers 1234567890 and strange \u0d78\u0bf5 symbols", ""),
            RName.Empty.sub("abc", "").sub("asdf123", "::"),
            RName.Empty.sub("arbitrary", "").sub("separators with", " spaces and $&*@ symbols "),
            RName.Empty.sub("a", "").sub("b", ".").sub("c", "::").sub("d", "$").sub("e", "_").sub("$", ".").sub("[]", "::"),
            RName.Empty.sub("", "").sub("", "").sub("", "").sub("", "").sub("", "")
        )

        val buffer = UnsafeBuffer(ByteArray(1000))
        for (name in testNames) {
            buffer.rewind()
            RNameMarshaller.write(buffer, name)
            buffer.rewind()
            val value = RNameMarshaller.read(buffer)

            assertTrue(rNameEquals(name, value)) { 
                "expected \"$name\" but got \"$value\""
            }
        }
    }
    
    
    private fun rNameEquals(a: RName, b: RName): Boolean {
        if (a == RName.Empty || b == RName.Empty)
            return a == b
        if (a.localName != b.localName || a.separator != b.separator)
            return false
        return rNameEquals(a.parent ?: RName.Empty, b.parent ?: RName.Empty)
    }
}