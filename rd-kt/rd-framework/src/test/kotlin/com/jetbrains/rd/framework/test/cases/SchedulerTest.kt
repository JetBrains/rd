package com.jetbrains.rd.framework.test.cases

import com.jetbrains.rd.framework.test.util.RdFrameworkTestBase
import com.jetbrains.rd.util.reactive.ExecutionOrder
import com.jetbrains.rd.util.spinUntil
import com.jetbrains.rd.util.threading.asRdScheduler
import com.jetbrains.rd.util.threading.asSequentialScheduler
import kotlinx.coroutines.*
import org.junit.jupiter.api.Test
import java.lang.IllegalStateException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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


    @Test
    fun sequentialSchedulerConcurrentTest() {

        val schedulers = sequence {
            for (i in 1 until 10)
                yield(Dispatchers.IO.limitedParallelism(i).asRdScheduler(ExecutionOrder.Unknown))

            yield(Dispatchers.IO.limitedParallelism(1).asRdScheduler(ExecutionOrder.Sequential))

            for (i in 1 until 10)
                yield(Dispatchers.IO.limitedParallelism(i).asRdScheduler(ExecutionOrder.OutOfOrder))
        }.map { it.asSequentialScheduler() }.toList()

        for (p in 0 .. 10) {
            runBlocking(Dispatchers.Default) {
                val signeThreadDispatcher  = Dispatchers.IO.limitedParallelism(1)

                schedulers.forEach { scheduler ->
                    launch {
                        var count = 0
                        val n = 1_000_00

                        coroutineScope {
                            for (j in 0 until 10) {
                                launch {
                                    for (i in 0 until n / 10) {

                                        scheduler.queue {
                                            assertTrue(scheduler.isActive)

                                            count++
                                        }

                                        if (i % 31 == 0)
                                            yield()
                                    }
                                }
                            }
                        }

                        withContext(signeThreadDispatcher) {
                            scheduler.flush()
                        }

                        assertEquals(n, count)
                    }
                }
            }
        }
    }

    @Test
    fun sequentialSchedulerKeepGlobalOrderTest() {
        val scheduler = Dispatchers.IO.limitedParallelism(1).asRdScheduler(ExecutionOrder.Unknown)
        val sequentialScheduler = scheduler.asSequentialScheduler()

        var count1 = 0
        var count2 = 0

        var hasErrors = false

        val n = 1_000_000
        for (i in 0 until n) {
            scheduler.queue {
                if (hasErrors)
                    return@queue

                try {
                    assertFalse(sequentialScheduler.isActive)
                    assertEquals(count1, count2)
                    count1++
                } catch (e: Throwable) {
                    hasErrors = true
                    throw e
                }
            }

            sequentialScheduler.queue {
                if (hasErrors)
                    return@queue
                try {
                    assertTrue(scheduler.isActive)
                    assertTrue(sequentialScheduler.isActive)

                    count2++
                    assertEquals(count1, count2)
                } catch (e: Throwable) {
                    hasErrors = true
                    throw e
                }
            }
        }
        sequentialScheduler.flush()
        assertEquals(count1, count2)
    }
}