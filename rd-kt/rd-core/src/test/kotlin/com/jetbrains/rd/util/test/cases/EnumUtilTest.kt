package com.jetbrains.rd.util.test.cases

import com.jetbrains.rd.util.parseFromFlags
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EnumUtilTest {

    enum class TestEnum {
        One,
        Two,
        Three
    }

    @Test
    fun testParseFromFlags() {
        val enumCombinedVal = (1 shl 2) or (1 shl 0) // like in C# TestEnum.Three | TestEnum.One
        val flags = parseFromFlags<TestEnum>(enumCombinedVal)
        assertTrue(flags.contains(TestEnum.Three))
        assertTrue(flags.contains(TestEnum.One))
        assertFalse(flags.contains(TestEnum.Two))
    }

    @Test
    fun testParseFromFlags_outOfRange() {
        Assertions.assertThrows(IllegalArgumentException::class.java) {
            val enumCombinedVal = (1 shl 3)
            parseFromFlags<TestEnum>(enumCombinedVal)
        }
    }

    @Test
    fun testParseFromFlags_maxPossibleValue() {
        val enumCombinedVal = (1 shl 3) - 1
        val flags = parseFromFlags<TestEnum>(enumCombinedVal)
        assertTrue(flags.size == 3)
    }
}