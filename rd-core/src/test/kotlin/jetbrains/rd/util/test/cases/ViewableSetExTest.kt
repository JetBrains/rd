package com.jetbrains.rider.util.test.cases

import com.jetbrains.rider.util.reactive.ViewableSet
import com.jetbrains.rider.util.reactive.createIsNotEmpty
import org.testng.annotations.Test
import kotlin.test.assertFalse

class ViewableSetExTest() {
    @Test
    fun createIsNotEmpty() {
        val xs = ViewableSet<Any>()
        val x = xs.createIsNotEmpty()
        assertFalse { x.value }
        xs.add(Any())
        assert(x.value)
        xs.clear()
        assertFalse { x.value }
    }

    @Test
    fun createIsNotEmpty2() {
        val element1 = Any()
        val element2 = Any()
        val element3 = Any()
        val ys = ViewableSet<Any>().apply { add(element1) }
        val y = ys.createIsNotEmpty()
        assert(y.value)
        ys.add(element2)
        assert(y.value)
        ys.remove(element1)
        assert(y.value)
        ys.add(element3)
        assert(y.value)
        ys.remove(element2)
        ys.remove(element3)
        assertFalse { y.value }
    }
}