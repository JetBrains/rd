package com.jetbrains.rider.util.test.cases

import com.jetbrains.rider.util.OfEqualItems
import com.jetbrains.rider.util.binSearch
import org.testng.Assert
import org.testng.annotations.Test

class BinSearchTest {
    @Test
    fun test01() {
        val list = listOf(1, 2, 3, 3, 3)
        val ret1 = list.binSearch(3, java.util.Comparator.comparingInt { it }, which = OfEqualItems.TakeLast)
        Assert.assertEquals(ret1, list.size - 1)

        val ret2 = list.binSearch(3, java.util.Comparator.comparingInt { it }, which = OfEqualItems.TakeFirst)
        Assert.assertEquals(ret2, 2)

        val ret3 = list.binSearch(3, java.util.Comparator.comparingInt { it }, which = OfEqualItems.TakeAny)
        Assert.assertEquals(ret3, 2)
    }

    @Test
    fun test02() {
        val list = listOf(1, 2, 3)
        var ret1 = list.binSearch(1, java.util.Comparator.comparingInt { it }, which = OfEqualItems.TakeLast)
        Assert.assertEquals(ret1, 0)

        var ret2 = list.binSearch(1, java.util.Comparator.comparingInt { it }, which = OfEqualItems.TakeFirst)
        Assert.assertEquals(ret2, 0)

        var ret3 = list.binSearch(1, java.util.Comparator.comparingInt { it }, which = OfEqualItems.TakeAny)
        Assert.assertEquals(ret3, 0)

        ret1 = list.binSearch(3, java.util.Comparator.comparingInt { it }, which = OfEqualItems.TakeLast)
        Assert.assertEquals(ret1, 2)

        ret2 = list.binSearch(3, java.util.Comparator.comparingInt { it }, which = OfEqualItems.TakeFirst)
        Assert.assertEquals(ret2, 2)

        ret3 = list.binSearch(3, java.util.Comparator.comparingInt { it }, which = OfEqualItems.TakeAny)
        Assert.assertEquals(ret3, 2)
    }

    @Test
    fun test03() {
        val list = listOf(1, 2, 4)
        val ret1 = list.binSearch(3, java.util.Comparator.comparingInt { it }, which = OfEqualItems.TakeLast)
        Assert.assertEquals(ret1, -(2 + 1))

        val ret2 = list.binSearch(3, java.util.Comparator.comparingInt { it }, which = OfEqualItems.TakeFirst)
        Assert.assertEquals(ret2, -(2 + 1))

        val ret3 = list.binSearch(3, java.util.Comparator.comparingInt { it }, which = OfEqualItems.TakeAny)
        Assert.assertEquals(ret3, -(2 + 1))
    }

    @Test
    fun test04() {
        val list = listOf(1, 2, 4)
        val ret1 = list.binSearch(5, java.util.Comparator.comparingInt { it }, which = OfEqualItems.TakeLast)
        Assert.assertEquals(ret1, -(list.size + 1))

        val ret2 = list.binSearch(5, java.util.Comparator.comparingInt { it }, which = OfEqualItems.TakeFirst)
        Assert.assertEquals(ret2, -(list.size + 1))

        val ret3 = list.binSearch(5, java.util.Comparator.comparingInt { it }, which = OfEqualItems.TakeAny)
        Assert.assertEquals(ret3, -(list.size + 1))
    }
}