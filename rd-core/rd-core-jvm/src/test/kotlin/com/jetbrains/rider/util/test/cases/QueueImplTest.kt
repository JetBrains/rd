package com.jetbrains.rider.util.test.cases

import com.jetbrains.rider.util.collections.QueueImpl
import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class QueueImplTest {

    private val q = QueueImpl<Int>()

    @BeforeMethod
    fun setup() {
        q.clear()
    }


    @Test(invocationCount = 2)
    fun testEmpty() {
        assert(q.isEmpty())
        q.offer(333)
        assert(!q.isEmpty())
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