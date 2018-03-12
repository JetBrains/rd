package com.jetbrains.rider.util.test.cases

import com.jetbrains.rider.util.collections.ImmutableStack
import com.jetbrains.rider.util.collections.tail
import com.jetbrains.rider.util.collections.toImmutableStack
import org.testng.annotations.Test
import kotlin.test.assertEquals


class ImmutableStackTest {
    @Test
    fun testBasic() {
        var s = ImmutableStack<Int>()
        assert(s.isEmpty)

        s.push(1)
        //naebalovo
        assert(s.isEmpty)

        s = s.push(1)
        assert(!s.isEmpty)
        assertEquals(1, s.peek())
        assertEquals(1, s.pop()?.second)
        assertEquals(1, s.pop()?.second)

        val e = ImmutableStack<Int>()
        assert(e.isEmpty)

        s = listOf(1,2).toImmutableStack()
        assertEquals(1, s.peek())

        s = s.tail()
        assertEquals(2, s.peek())

        s = s.tail()
        assertEquals(null, s.peek())
    }
}