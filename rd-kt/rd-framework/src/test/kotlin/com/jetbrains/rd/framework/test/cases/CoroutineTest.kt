package com.jetbrains.rd.framework.test.cases

import com.jetbrains.rd.framework.util.*
import com.jetbrains.rd.util.error
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.lifetime.LifetimeStatus
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.reactive.Signal
import com.jetbrains.rd.util.spinUntil
import com.jetbrains.rd.util.threading.CompoundThrowable
import kotlinx.coroutines.*
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.coroutines.CoroutineContext
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CoroutineTest : CoroutineTestBase() {

    @Test
    fun simpleRdCoroutineHostTest() {
        runBlocking {
            var value = RdCoroutineScope.current.async(lifetime) {
                scheduler.assertThread()
                delay(1)
                scheduler.assertThread()
                1
            }.await()

            assertEquals(1, value)

            RdCoroutineScope.current.launch(lifetime) {
                scheduler.assertThread()
                delay(1)
                scheduler.assertThread()
                value++
            }.join()

            assertEquals(2, value)
        }
    }

    @Test
    fun handleExceptionsTest() {
        val job = RdCoroutineScope.current.launch(lifetime) {
            throw TestException()
        }

        spinUntil { job.isCompleted }
        val exceptions = host.getExceptions(true)
        if (exceptions.isEmpty())
            throw IllegalStateException("exceptions.isEmpty()")
        if (exceptions.size > 1)
        {
            val unexpectedExceptions = exceptions.filter { it !is TestException }
            if (unexpectedExceptions.isEmpty()) {
                throw IllegalStateException("Count of test exceptions should not be greater than 1")
            }
            throw CompoundThrowable(unexpectedExceptions)
        }

        val throwable = exceptions.single()
        if (throwable !is TestException)
            throw throwable
    }

    @Test
    fun nextValueAsyncTest() {
        runBlocking {
            val signal = Signal<String?>()

            kotlin.run {
                val task = signal.nextValueAsync(lifetime)
                signal.fire("1")
                assertEquals("1", task.await())
            }

            kotlin.run {
                val task = signal.nextValueAsync(lifetime) { it == "2" }
                signal.fire("1")
                assert(!task.isCompleted)
                signal.fire("2")
                assert(task.isCompleted)
                assertEquals("2", task.await())
            }

            kotlin.run {
                val task = signal.nextNotNullValueAsync(lifetime)
                signal.fire(null)
                assert(!task.isCompleted)
                signal.fire("1")
                assert(task.isCompleted)
                assertEquals("1", task.await())
            }
        }

        runBlocking {
            val signal = Signal<Boolean>()

            kotlin.run {
                val task = signal.nextTrueValueAsync(lifetime)
                signal.fire(false)
                assert(!task.isCompleted)
                signal.fire(true)
                assert(task.isCompleted)
                assert(task.await())
            }

            kotlin.run {
                val task = signal.nextFalseValueAsync(lifetime)
                signal.fire(true)
                assert(!task.isCompleted)
                signal.fire(false)
                assert(task.isCompleted)
                assertFalse(task.await())
            }
        }
    }

    @Test
    fun nextValueTest() {
        runBlocking(scheduler.asCoroutineDispatcher) {
            val signal = Signal<String?>()

            kotlin.run {
                val task = async {
                    scheduler.assertThread()
                    signal.nextValue(lifetime)
                }
                yield()
                signal.fire("1")
                yield()
                assert(task.isCompleted)
                assertEquals("1", task.await())
            }

            kotlin.run {
                val task = async {
                    scheduler.assertThread()
                    signal.nextValue(lifetime) { it == "2" }
                }
                yield()
                signal.fire("1")
                yield()
                assert(!task.isCompleted)
                signal.fire("2")
                yield()
                assert(task.isCompleted)
                assertEquals("2", task.await())
            }

            kotlin.run {
                val task = async {
                    scheduler.assertThread()
                    signal.nextNotNullValue(lifetime)
                }
                yield()
                signal.fire(null)
                yield()
                assert(!task.isCompleted)
                signal.fire("1")
                yield()
                assert(task.isCompleted)
                assertEquals("1", task.await())
            }
        }

        runBlocking(scheduler.asCoroutineDispatcher) {
            val signal = Signal<Boolean>()

            kotlin.run {
                val task = async {
                    scheduler.assertThread()
                    signal.nextTrueValue(lifetime)
                }
                yield()
                signal.fire(false)
                yield()
                assert(!task.isCompleted)
                signal.fire(true)
                yield()
                assert(task.isCompleted)
                assert(task.await())
            }

            kotlin.run {
                val task = async {
                    scheduler.assertThread()
                    signal.nextFalseValue(lifetime)
                }
                yield()
                signal.fire(true)
                yield()
                assert(!task.isCompleted)
                signal.fire(false)
                yield()
                assert(task.isCompleted)
                assertFalse(task.await())
            }
        }
    }

    @Test
    fun nonBlockingWaitForTest() {
        runBlocking(scheduler.asCoroutineDispatcher) {
            scheduler.assertThread()
            var a = 1
            val task = async {
                scheduler.assertThread()
                assertEquals(1, a)

                lifetime.waitFor(Duration.ofSeconds(10)) {
                    scheduler.assertThread()
                    if (a == 1) return@waitFor false
                    if (a == 2) {
                        a++
                        return@waitFor false
                    }
                    return@waitFor a == 4
                }
                scheduler.assertThread()
                assertEquals(4, a)
                return@async a + 1
            }

            yield()
            a = 2

            while (!task.isCompleted) {
                scheduler.assertThread()
                if (a == 3) a++
                delay(10)
            }

            scheduler.assertThread()
            assert(task.isCompleted)
            assertEquals(5, task.await())
        }
    }

    @Test
    fun lifetimeStartTest() {
        runBlocking {
            val thread = lifetime.startAsync(scheduler.executor.asCoroutineDispatcher()) {
                var count = 0
                val t = Thread.currentThread()
                launch {
                    assertEquals(1, count)
                    assertEquals(t, Thread.currentThread())
                    count = 2
                }

                count = 1
                yield()
                assertEquals(t, Thread.currentThread())
                assertEquals(2, count)
                return@startAsync t
            }.await()

            lifetime.startAsync(scheduler) {
                var count = 0
                assertEquals(thread, Thread.currentThread())
                scheduler.assertThread()
                launch {
                    assertEquals(1, count)
                    assertEquals(thread, Thread.currentThread())
                    scheduler.assertThread()
                    count = 2
                }

                count = 1
                yield()
                assertEquals(thread, Thread.currentThread())
                scheduler.assertThread()
                assertEquals(2, count)
                return@startAsync thread
            }.join()

            lifetime.startAsync(scheduler.executor.asCoroutineDispatcher()) {
                var count = 0
                assertEquals(thread, Thread.currentThread())
                launch {
                    assertEquals(1, count)
                    assertEquals(thread, Thread.currentThread())
                    count = 2
                }

                count = 1
                yield()
                assertEquals(thread, Thread.currentThread())
                assertEquals(2, count)
                return@startAsync thread
            }.join()

            lifetime.launch(scheduler.executor.asCoroutineDispatcher()) {
                var count = 0
                val t = Thread.currentThread()
                launch {
                    assertEquals(1, count)
                    assertEquals(t, Thread.currentThread())
                    count = 2
                }

                count = 1
                yield()
                assertEquals(t, Thread.currentThread())
                assertEquals(2, count)
            }.join()

            lifetime.launch(scheduler) {
                var count = 0
                assertEquals(thread, Thread.currentThread())
                scheduler.assertThread()
                launch {
                    assertEquals(1, count)
                    assertEquals(thread, Thread.currentThread())
                    scheduler.assertThread()
                    count = 2
                }

                count = 1
                yield()
                assertEquals(thread, Thread.currentThread())
                scheduler.assertThread()
                assertEquals(2, count)
            }.join()

            lifetime.launch(scheduler.executor.asCoroutineDispatcher()) {
                var count = 0
                assertEquals(thread, Thread.currentThread())
                launch {
                    assertEquals(1, count)
                    assertEquals(thread, Thread.currentThread())
                    count = 2
                }

                count = 1
                yield()
                assertEquals(thread, Thread.currentThread())
                assertEquals(2, count)
            }.join()
        }
    }

    @Test
    fun synchronizeWithTest() {
        run {
            val def = LifetimeDefinition()
            val job = RdCoroutineScope.current.launch {
                while (true)
                    delay(100)
            }

            def.synchronizeWith(job)

            def.terminate()
            assert(job.isCancelled)
        }

        runBlocking {
            val def = LifetimeDefinition()
            val job = RdCoroutineScope.current.launch {
                while (true)
                    delay(100)
            }

            def.synchronizeWith(job)

            try {
                job.cancelAndJoin()
            } catch (e: CancellationException) {
                //ok
            }

            assert(def.status == LifetimeStatus.Terminated)
        }

        runBlocking {
            val def = LifetimeDefinition()
            val job = RdCoroutineScope.current.async {
                delay(1)
                1
            }

            def.synchronizeWith(job)

            val value = job.await()
            assertEquals(1, value)
            assert(def.status == LifetimeStatus.Terminated)
        }
    }

    @Test
    fun withContextTest() {
        runBlocking {
            assert(!scheduler.isActive)

            val value = Lifetime.using { lifetime ->
                withContext(lifetime, scheduler.asCoroutineDispatcher) {
                    scheduler.assertThread()

                    delay(10)
                    scheduler.assertThread()
                    1
                }
            }

            assertEquals(1, value)
        }

        runBlocking {
            assert(!scheduler.isActive)

            val value = Lifetime.using { lifetime ->
                withContext(lifetime, scheduler) {
                    scheduler.assertThread()

                    delay(10)
                    scheduler.assertThread()
                    1
                }
            }

            assertEquals(1, value)
        }

        runBlocking {
            assert(!scheduler.isActive)

            val value = withContext(scheduler) {
                scheduler.assertThread()

                delay(10)
                scheduler.assertThread()
                1
            }

            assertEquals(1, value)
        }
    }

    @Test
    fun withContextCancellationTest() {
        runBlocking {
            assert(!scheduler.isActive)

            val def = LifetimeDefinition()
            val job = async {
                try {
                    withContext(def.lifetime, scheduler.asCoroutineDispatcher) {
                        scheduler.assertThread()

                        def.terminate()
                        delay(100)
                    }

                    logger.error { "Must not be reached" }
                } catch (e: CancellationException) {
                    // ok
                }

                1
            }

            assertEquals(1, job.await())
            assertEquals(LifetimeStatus.Terminated, def.status)
        }

        runBlocking {
            assert(!scheduler.isActive)

            val def = LifetimeDefinition()
            val job = async {
                try {
                    withContext(def.lifetime, scheduler) {
                        scheduler.assertThread()

                        def.terminate()
                        delay(100)
                    }

                    logger.error { "Must not be reached" }
                } catch (e: CancellationException) {
                    // ok
                }

                1
            }

            assertEquals(1, job.await())
            assertEquals(LifetimeStatus.Terminated, def.status)
        }
    }

    @Test
    fun asCoroutineDispatcherTest() {
        val realScheduler = object : IScheduler {
            override fun queue(action: () -> Unit) = scheduler.queue(action)
            override val isActive get() = scheduler.isActive
            override fun flush() = TODO("Not yet implemented")
        }

        fun doNonInlineTest(dispatcher: CoroutineDispatcher) {
            runBlocking(dispatcher) {
                realScheduler.assertThread()

                var value = 0
                val job = async {
                    realScheduler.assertThread()
                    assertEquals(1, value)
                    value = 2

                    yield()
                    realScheduler.assertThread()
                    assertEquals(3, value)
                    4
                }

                realScheduler.assertThread()
                assertEquals(0, value)
                value = 1

                yield()
                assertEquals(2, value)
                value = 3

                assertEquals(4, job.await())
            }

        }

        fun doInlineTest(dispatcher: CoroutineDispatcher) {
            runBlocking(dispatcher) {
                realScheduler.assertThread()

                var value = 0
                val job = async {
                    realScheduler.assertThread()
                    assertEquals(0, value)
                    value = 1

                    yield()
                    realScheduler.assertThread()
                    assertEquals(2, value)
                    3
                }

                realScheduler.assertThread()
                assertEquals(1, value)
                value = 2

                assertEquals(3, job.await())
            }
        }

        doInlineTest(realScheduler.asCoroutineDispatcher(allowInlining = true))

        doNonInlineTest(realScheduler.asCoroutineDispatcher(allowInlining = false))
        doNonInlineTest(realScheduler.asCoroutineDispatcher)


        val inliningScheduler = object : CoroutineDispatcher(), IScheduler {
            override fun queue(action: () -> Unit) = scheduler.queue(action)
            override val isActive get() = scheduler.isActive
            override fun flush() = TODO("Not yet implemented")

            override fun dispatch(context: CoroutineContext, block: Runnable) = queue { block.run() }
            override fun isDispatchNeeded(context: CoroutineContext) = !isActive
        }

        val nonInliningScheduler = object : CoroutineDispatcher(), IScheduler {
            override fun queue(action: () -> Unit) = scheduler.queue(action)
            override val isActive get() = scheduler.isActive
            override fun flush() = TODO("Not yet implemented")

            override fun dispatch(context: CoroutineContext, block: Runnable) = queue { block.run() }
            override fun isDispatchNeeded(context: CoroutineContext) = true
        }

        doInlineTest(inliningScheduler.asCoroutineDispatcher)
        doNonInlineTest(nonInliningScheduler.asCoroutineDispatcher)
    }

    private class TestException : Exception()
}