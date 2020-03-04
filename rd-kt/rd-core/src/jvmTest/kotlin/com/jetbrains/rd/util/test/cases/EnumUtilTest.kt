package com.jetbrains.rd.util.test.cases

import com.jetbrains.rd.util.parseFromFlags
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

    @Test(expected = IllegalArgumentException::class)
    fun testParseFromFlags_outOfRange() {
        val enumCombinedVal = (1 shl 3)
        parseFromFlags<TestEnum>(enumCombinedVal)
    }

    @Test
    fun testParseFromFlags_maxPossibleValue() {
        val enumCombinedVal = (1 shl 3) - 1
        val flags = parseFromFlags<TestEnum>(enumCombinedVal)
        assertTrue(flags.size == 3)
    }
}