package com.jetbrains.rider.util.test.cases

import org.testng.annotations.Test
import com.jetbrains.rider.util.threading.TestSingleThreadScheduler
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackgroundSchedulerTest {
    @Test
    fun test0() {
        val s = TestSingleThreadScheduler("test")
        assertFalse { s.isActive }
        assertFails { s.assertThread() }

        var x : Int = 0
        s.queue {
            Thread.sleep(100)
            x++
        }
        s.queue {
            x++
            s.assertThread()
        }
        assertTrue("x == 0", { x == 0 })

        s.assertNoExceptions()
        assertTrue("x == 2", { x == 2 })

        s.queue {
            throw IllegalStateException()
        }
        s.queue {
            x++
        }

        assertFails { s.assertNoExceptions() }
        assertTrue("x == 3", { x == 3 })
    }
}

