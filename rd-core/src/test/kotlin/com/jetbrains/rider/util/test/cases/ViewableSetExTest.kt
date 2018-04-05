package com.jetbrains.rider.util.test.cases
import com.jetbrains.rider.util.reactive.ViewableSet
import com.jetbrains.rider.util.reactive.createIsNotEmpty
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ViewableSetExTest() {
    @Test
    fun createIsNotEmpty() {
        val xs = ViewableSet<Any>()
        val x = xs.createIsNotEmpty()
        assertFalse { x.value }
        xs.add(Any())
        assertTrue(x.value)
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
        assertTrue(y.value)
        ys.add(element2)
        assertTrue(y.value)
        ys.remove(element1)
        assertTrue(y.value)
        ys.add(element3)
        assertTrue(y.value)
        ys.remove(element2)
        ys.remove(element3)
        assertFalse { y.value }
    }
}