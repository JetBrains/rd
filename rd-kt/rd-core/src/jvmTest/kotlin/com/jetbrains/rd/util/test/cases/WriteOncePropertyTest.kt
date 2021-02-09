package com.jetbrains.rd.util.test.cases

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.isAlive
import com.jetbrains.rd.util.reactive.WriteOnceProperty
import com.jetbrains.rd.util.spinUntil
import com.jetbrains.rd.util.test.framework.RdTestBase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.stream.IntStream.range
import kotlin.test.*

class WriteOncePropertyTest : RdTestBase() {

    @Test
    fun simpleTest() {
        Lifetime.using { lifetime ->
            val prop = WriteOnceProperty<Int>()

            var v1 = 0
            prop.advise(lifetime) { value ->
                v1 = value
            }

            assertEquals(0, v1)

            var v2 = 0
            prop.view(lifetime) { lf, value ->
                assert(lf.isAlive)
                v2 = value
            }

            assertEquals(0, v1)
            assertEquals(0, v2)
            assertEquals(null, prop.valueOrNull)

            assertTrue(prop.setIfEmpty(1))
            assertEquals(1, v1)
            assertEquals(1, v2)
            assertEquals(1, prop.valueOrNull)

            assertFalse(prop.setIfEmpty(2))
            assertEquals(1, v1)
            assertEquals(1, v2)
            assertEquals(1, prop.valueOrNull)

            try {
                prop.set(2)
                fail("must not be reached")
            } catch (e: IllegalStateException) {
                // ok
            }

            assertEquals(1, v1)
            assertEquals(1, v2)
            assertEquals(1, prop.valueOrNull)


            var v3 = 0
            prop.advise(lifetime) { value ->
                v3 = value
            }

            assertEquals(1, v1)
            assertEquals(1, v2)
            assertEquals(1, v3)
            assertEquals(1, prop.valueOrNull)

            var v4 = 0
            prop.view(lifetime) { lf, value ->
                assert(lf.isAlive)
                v4 = value
            }

            assertEquals(1, v1)
            assertEquals(1, v2)
            assertEquals(1, v3)
            assertEquals(1, v4)
            assertEquals(1, prop.valueOrNull)

            prop.fireInternal(3)

            assertEquals(1, v1)
            assertEquals(1, v2)
            assertEquals(1, v3)
            assertEquals(1, v4)
            assertEquals(1, prop.valueOrNull)
        }
    }

    @Test
    fun viewTest() {
        Lifetime.using { lifetime ->
            val prop = WriteOnceProperty<Int>()

            var viewedValue = 0
            prop.view(lifetime) { lf, value ->
                viewedValue = value
                assert(lifetime === lf) // reference equals
            }

            prop.set(1)
            assertEquals(1, viewedValue)
        }
    }

    @Test
    fun concurrentWriteTest() {
        val threadsCount = 10
        withThreadPool(threadsCount) { dispatcher ->

            for (i in 0 until 200) {

                Lifetime.using { lifetime ->
                    val prop = WriteOnceProperty<Int>()

                    val value1 = AtomicReference<Int?>(null)
                    prop.advise(lifetime) {
                        if (!value1.compareAndSet(null, it)) {
                            fail("Handled must not be called twice")
                        }
                    }

                    val value2 = AtomicReference<Int?>(null)

                    val count = AtomicInteger(0)
                    runBlocking {
                        for (j in 0 until threadsCount) {
                            launch(dispatcher) {

                                count.incrementAndGet()
                                spinUntil { count.get() == threadsCount } // sync threads

                                if (!prop.setIfEmpty(j)) return@launch

                                if (!value2.compareAndSet(null, j)) {
                                    fail("Value must be written once")
                                }
                            }
                        }
                    }

                    assertNotNull(value1.get())
                    assertNotNull(value2.get())

                    assertEquals(value1.get(), value2.get())
                    assertEquals(value1.get(), prop.valueOrNull)

                    prop.fireInternal(1000)
                }
            }

        }
    }

    @Test
    fun concurrentWriteAndAdviseTest() {
        val threadsCount = 10
        withThreadPool(threadsCount) { dispatcher ->

            for (i in 0 until 200) {

                Lifetime.using { lifetime ->
                    val prop = WriteOnceProperty<Int>()

                    val value1 = AtomicReference<Int?>(null)
                    val refs = runBlocking {
                        val count = AtomicInteger(0)
                        for (j in 0 until threadsCount) {
                            launch(dispatcher) {

                                count.incrementAndGet()
                                spinUntil { count.get() == threadsCount } // sync threads

                                if (!prop.setIfEmpty(j)) return@launch

                                if (!value1.compareAndSet(null, j)) {
                                    fail("Value must be written once")
                                }
                            }
                        }


                        range(0, i).toArray().map {
                            val localValue = AtomicReference<Int?>(null)
                            prop.advise(lifetime) {
                                if (!localValue.compareAndSet(null, it)) {
                                    fail("Handled must not be called twice")
                                }
                            }
                            localValue
                        }
                    }.map { it.get() }


                    assertNotNull(value1.get())

                    if (refs.isNotEmpty()) {
                        val value = refs.distinct().single()
                        assertEquals(value, value1.get())
                    }

                    assertEquals(value1.get(), prop.valueOrNull)

                    prop.fireInternal(10000)
                }
            }
        }
    }

    private fun withThreadPool(threadsCount: Int, action: (CoroutineDispatcher) -> Unit) {
        val threadPool = Executors.newFixedThreadPool(threadsCount)
        try {
            val dispatcher = threadPool.asCoroutineDispatcher()
            action(dispatcher)
        } finally {
            threadPool.shutdown()
            spinUntil { threadPool.isTerminated }
        }
    }


}