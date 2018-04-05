package com.jetbrains.rider.util.test.cases
import com.jetbrains.rider.util.collections.ImmutableStack
import com.jetbrains.rider.util.collections.tail
import com.jetbrains.rider.util.collections.toImmutableStack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue



class ImmutableStackTest {
    @Test
    fun testBasic() {
        var s = ImmutableStack<Int>()
        assertTrue(s.isEmpty)

        s.push(1)
        //naebalovo
        assertTrue(s.isEmpty)

        s = s.push(1)
        assertTrue(!s.isEmpty)
        assertEquals(1, s.peek())
        assertEquals(1, s.pop()?.second)
        assertEquals(1, s.pop()?.second)

        val e = ImmutableStack<Int>()
        assertTrue(e.isEmpty)

        s = listOf(1,2).toImmutableStack()
        assertEquals(1, s.peek())

        s = s.tail()
        assertEquals(2, s.peek())

        s = s.tail()
        assertEquals(null, s.peek())
    }
}