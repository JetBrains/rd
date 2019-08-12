package com.jetbrains.rd.util.test.cases

import com.jetbrains.rd.util.AtomicInteger
import com.jetbrains.rd.util.Closeable
import com.jetbrains.rd.util.ILoggerFactory
import com.jetbrains.rd.util.Statics
import com.jetbrains.rd.util.log.ErrorAccumulatorLoggerFactory
import com.jetbrains.rd.util.threading.TestSingleThreadScheduler
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse

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


        val tasksExecuted = AtomicInteger(0)
        s.queue {
            Thread.sleep(100)
            tasksExecuted.incrementAndGet()
        }
        s.queue {
            tasksExecuted.incrementAndGet()
            s.assertThread()
        }
        assertEquals(0, tasksExecuted.get())

        s.assertNoExceptions()

        assertEquals(2, tasksExecuted.get())

        s.queue {
            throw IllegalStateException()
        }
        s.queue {
            tasksExecuted.incrementAndGet()
        }

        assertFails { s.assertNoExceptions() }
        assertEquals(tasksExecuted.get(), 3)
    }
}

