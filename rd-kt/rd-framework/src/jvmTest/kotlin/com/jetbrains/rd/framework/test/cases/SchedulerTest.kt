package com.jetbrains.rd.framework.test.cases

import com.jetbrains.rd.framework.test.util.RdFrameworkTestBase
import com.jetbrains.rd.util.spinUntil
import com.jetbrains.rd.util.threading.asRdScheduler
import org.junit.jupiter.api.Test
import java.lang.IllegalStateException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SchedulerTest : RdFrameworkTestBase() {

    private fun createThreadFactory() = ThreadFactory { r ->
        Thread(r, "Test Background Thread")
            .apply { isDaemon = true }
            .apply { priority = Thread.NORM_PRIORITY }

    }

    @Test
    fun asRdSchedulerTest() {
        val n = 10
        val executor = object : ThreadPoolExecutor(n, n, 0L, TimeUnit.MILLISECONDS, LinkedBlockingQueue(), createThreadFactory()) { }
        val scheduler = executor.asRdScheduler
        assertFalse(scheduler.isActive)

        val count = AtomicInteger()
        for (i in 0 until n) {
            scheduler.queue {
                count.incrementAndGet()
                assert(scheduler.isActive)
                spinUntil(1000) { count.get() == n }
                assertEquals(n, count.get())

                try {
                    scheduler.flush()
                    throw IllegalStateException("This point must not be reached")
                } catch (e: IllegalArgumentException) {
                    // ok
                }
            }
        }

        scheduler.flush()
        assertEquals(n, count.get())
    }
}