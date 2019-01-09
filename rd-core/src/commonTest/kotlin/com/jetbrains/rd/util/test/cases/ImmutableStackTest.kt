package com.jetbrains.rd.util.test.cases
import com.jetbrains.rd.util.collections.ImmutableStack
import com.jetbrains.rd.util.collections.tail
import com.jetbrains.rd.util.collections.toImmutableStack
import com.jetbrains.rd.util.test.framework.RdTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue



class ImmutableStackTest : RdTestBase()  {
    @Test
    fun testBasic() {
        var s = ImmutableStack<Int>()
        assertTrue(s.isEmpty)

        s.push(1)
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