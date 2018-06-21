package com.jetbrains.rider.util.test.cases

import com.jetbrains.rider.util.threading.TestSingleThreadScheduler
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackgroundSchedulerTest {
    @Test
    fun test0() {
        val s = TestSingleThreadScheduler("test")
        assertFalse { s.isActive }
        assertFails { s.assertThread() }

        var tasksExecuted : Int = 0
        s.queue {
            Thread.sleep(100)
            tasksExecuted++
        }
        s.queue {
            tasksExecuted++
            s.assertThread()
        }
        assertEquals(0, tasksExecuted)

        s.assertNoExceptions()
        assertEquals(2, tasksExecuted)

        s.queue {
            throw IllegalStateException()
        }
        s.queue {
            tasksExecuted++
        }

        assertFails { s.assertNoExceptions() }
        assertTrue("x == 3", { tasksExecuted == 3 })
    }
}

