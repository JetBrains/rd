package com.jetbrains.rider.util.test.cases

import com.jetbrains.rider.util.collections.OrderedSet
import org.testng.Assert
import org.testng.annotations.Test

class OrderedSetTest {
    @Test
    fun test1() {
        val set = OrderedSet<Int>()
        set.addAll(arrayOf(1, 2, 3))
        Assert.assertEquals(set, listOf(1, 2, 3))
    }

    @Test
    fun test2() {
        val set = OrderedSet<Int>()
        set.addAll(arrayOf(1, 2, 3))
        Assert.assertEquals(set.add(1), false)
        set.remove(1)
        Assert.assertEquals(set.contains(1), false)
    }

    @Test
    fun test3() {
        val set = OrderedSet<Int>()
        set.addAll(arrayOf(1, 2, 3))

        val it = set.iterator()
        it.next()
        it.remove()
        Assert.assertEquals(set, listOf(2, 3))
        Assert.assertEquals(set.contains(1), false)
    }

    @Test
    fun test4() {
        val set = OrderedSet<Int>()
        set.add(0, 1)
        set.add(0, 2)
        set.add(0, 3)
        set.add(0)
        Assert.assertEquals(set, listOf(3, 2, 1, 0))
        var res = true
        for (i in 0..3) {
            res = res && set.contains(i)
            Assert.assertEquals(res, true)
        }
    }

    @Test
    fun test5() {
        val set = OrderedSet<Int>()
        set.addAll(arrayOf(1, 2, 3))
        set[2] = 4
        Assert.assertEquals(set, listOf(1, 2, 4))
        set.removeAll(arrayOf(1, 2, 4))
        Assert.assertEquals(set, listOf<Int>())
        for (i in 0..4) {
            Assert.assertEquals(set.contains(i), false)
        }
    }

    @Test(expectedExceptions = arrayOf(IllegalArgumentException::class))
    fun test6() {
        val set = OrderedSet<Int>()
        set.add(0)
        set.add(0, 0)
    }
}