package com.jetbrains.rider.util.test.cases

import com.jetbrains.rider.util.Closeable
import com.jetbrains.rider.util.ILoggerFactory
import com.jetbrains.rider.util.Statics
import com.jetbrains.rider.util.log.ErrorAccumulatorLoggerFactory
import com.jetbrains.rider.util.threading.TestSingleThreadScheduler
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackgroundSchedulerTest {


    private lateinit var disposeLoggerFactory: Closeable

    @Before
    fun setup() {
        val statics = Statics<ILoggerFactory>()
        disposeLoggerFactory = statics.push(ErrorAccumulatorLoggerFactory)
    }

    @After
    fun tearDown() {
        disposeLoggerFactory.close()
    }



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

