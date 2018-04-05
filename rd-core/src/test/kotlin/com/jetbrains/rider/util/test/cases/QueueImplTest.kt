package com.jetbrains.rider.util.test.cases

import com.jetbrains.rider.util.collections.QueueImpl
import kotlin.test.*

class QueueImplTest {

    private val q = QueueImpl<Int>()

    @BeforeTest
    fun setup() {
        q.clear()
    }


    @Test
    fun testEmpty() {
        assertTrue(q.isEmpty())
        q.offer(333)
        assertTrue(!q.isEmpty())
    }

    @Test
    fun testBasic() {
        q.offer(0)
        q.offer(1)
        q.offer(2)

        assertEquals(0, q.peek())
        assertEquals(0, q.peek())

        assertEquals(0, q.poll())
        assertEquals(1, q.poll())

        q.offer(3)

        assertEquals(2, q.poll())
        assertEquals(3, q.poll())

        assertNull(q.poll())
        assertNull(q.peek())
    }


}