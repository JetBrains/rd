package com.jetbrains.rd.util.test.cases

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.isAlive
import com.jetbrains.rd.util.reactive.*
import com.jetbrains.rd.util.test.framework.RdTestBase
import kotlinx.coroutines.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class FlowSupportTest : RdTestBase() {

    @Test
    fun asNullableCollectConcurrentTest() = runBlockingWithTimeout {
        val n = 1
        var optProperty = OptProperty<Int>()

        Lifetime.using { lifetime ->
            runInParallelAsync(lifetime) {
                val job = coroutineContext.job
                var prev: Int? = -1
                optProperty.asNullable().asFlow().collect {

                    if (it == null) assertEquals(-1, prev)

                    if (prev == -1) {
                        prev = it

                        if (it == n) job.cancel()

                        return@collect
                    }

                    when (it) {
                        null -> assertEquals(-1, prev)
                        0 -> assertEquals(null, prev)
                        else -> assertEquals(it - 1, prev)
                    }

                    prev = it
                    if (it == n) job.cancel()
                }
            }

            for (i in 0..6_000_000) {
                for (j in 0..n) {
                    optProperty.set(j)
                }

                optProperty = OptProperty()
                yield()
            }
            for (i in 0..n) {
                optProperty.set(i)
            }

            yield()
        }
    }

    @Test
    fun optPropertyCollectConcurrentTest() = runBlockingWithTimeout {
        val n = 5
        var optProperty = OptProperty<Int>()

        Lifetime.using { lifetime ->
            runInParallelAsync(lifetime) {
                val job = coroutineContext.job
                var prev: Int? = -1
                optProperty.asFlow().collect {
                    if (prev != -1)
                        assertEquals(it - 1, prev)

                    prev = it
                    if (it == n)
                        job.cancel()
                }

            }

            for (i in 0..1_000_000) {
                for (j in 0..n) {
                    optProperty.set(j)
                }

                optProperty = OptProperty()
                yield()
            }
            for (i in 0..n) {
                optProperty.set(i)
            }
        }
    }

    @Test
    fun optPropertyMapCollectConcurrentTest() = runBlockingWithTimeout {
        val n = 5
        var optProperty = OptProperty<Int>()

        Lifetime.using { lifetime ->
            runInParallelAsync(lifetime) {
                val job = coroutineContext.job
                var prev: Int? = -1
                optProperty.map { it.toString() }.map { it.toInt() }.asFlow().collect {
                    if (prev != -1)
                        assertEquals(it - 1, prev)

                    prev = it
                    if (it == n)
                        job.cancel()
                }

            }

            for (i in 0..1_000_000) {
                for (j in 0..n) {
                    optProperty.set(j)
                }

                optProperty = OptProperty()
                yield()
            }
            for (i in 0..n) {
                optProperty.set(i)
            }
        }
    }

    @Test
    fun optPropertyFilterCollectConcurrentTest() = runBlockingWithTimeout {
        val n = 5
        var optProperty = OptProperty<Int>()

        Lifetime.using { lifetime ->
            runInParallelAsync(lifetime) {
                val job = coroutineContext.job
                var prev: Int? = -1
                optProperty.filter { true }.asFlow().collect {
                    if (prev != -1)
                        assertEquals(it - 1, prev)

                    prev = it
                    if (it == n)
                        job.cancel()
                }
            }

            for (i in 0..1_000_000) {
                for (j in 0..n) {
                    optProperty.set(j)
                }

                optProperty = OptProperty()
                yield()
            }
            for (i in 0..n) {
                optProperty.set(i)
            }
        }
    }

    @Test
    fun propertyCollectConcurrentTest() = runBlockingWithTimeout {
        val n = 5
        var property = Property(0)

        Lifetime.using { lifetime ->
            runInParallelAsync(lifetime) {
                val job = coroutineContext.job
                var prev: Int? = -1
                property.asFlow().collect {
                    if (prev != -1)
                        assertEquals(it - 1, prev)

                    prev = it
                    if (it == n)
                        job.cancel()
                }

            }
            for (i in 0..1_000_000) {
                for (j in 0..n) {
                    property.set(j)
                }

                property = Property(0)
                yield()
            }

            for (i in 0..n) {
                property.set(i)
            }
        }
    }

    @Test
    fun propertyMapCollectConcurrentTest() = runBlockingWithTimeout {
        val n = 5
        var property = Property(0)

        Lifetime.using { lifetime ->
            runInParallelAsync(lifetime) {
                val job = coroutineContext.job
                var prev: Int? = -1
                property.map { it.toString() }.map { it.toInt() }.asFlow().collect {
                    if (prev != -1)
                        assertEquals(it - 1, prev)

                    prev = it
                    if (it == n)
                        job.cancel()
                }

            }
            for (i in 0..1_000_000) {
                for (j in 0..n) {
                    property.set(j)
                }

                property = Property(0)
                yield()
            }

            for (i in 0..n) {
                property.set(i)
            }
        }
    }

    @Test
    fun propertyFilterCollectConcurrentTest() = runBlockingWithTimeout {
        val n = 5
        var property = Property(0)

        Lifetime.using { lifetime ->
            runInParallelAsync(lifetime) {
                val job = coroutineContext.job
                var prev: Int? = -1
                property.filter { true }.asFlow().collect {
                    if (prev != -1)
                        assertEquals(it - 1, prev)

                    prev = it
                    if (it == n)
                        job.cancel()
                }

            }
            for (i in 0..1_000_000) {
                for (j in 0..n) {
                    property.set(j)
                }

                property = Property(0)
                yield()
            }

            for (i in 0..n) {
                property.set(i)
            }
        }
    }

    @Test
    fun propertySwitchMapCollectConcurrentTest() = runBlockingWithTimeout {
        val n = 5
        var property = Property(Property(0))

        Lifetime.using { lifetime ->
            runInParallelAsync(lifetime) {thread ->
                val job = coroutineContext.job
                var prev: Int? = -1
                property.switchMap { it }.asFlow().collect { value ->
                    if (prev != -1)
                        assertEquals(value - 1, prev)

                    prev = value
                    if (value == n)
                        job.cancel()
                }

            }
            for (i in 0..1_000_000) {
                for (j in 0..n) {
                    property.value.set(j)
                }

                property = Property(Property(0))
                yield()
            }

            for (i in 0..n) {
                property.value.set(i)
            }
        }
    }

    @Test
    fun collectTonsOfItems() {
        val list = ViewableList<Int>()
        val n = 1_000_000
        for (i in 0 .. n) {
            list.add(i)
        }

        runBlocking {
            launch {
                val scope = this
                var prev = -1
                list.asFlow().collect {
                    val value = it.newValueOpt!!
                    assertEquals(it.index, value)
                    assertEquals(value - 1, prev)
                    prev = value

                    if (prev == n)
                        scope.cancel()
                }
            }
        }
    }

    @Test
    fun composePropertyCollectConcurrentTest() = runBlockingWithTimeout {
        val n = 5
        var optProperty1 = Property(0)
        var optProperty2 = Property(0)

        var composeProperty = optProperty1.compose(optProperty2) { l, r -> listOf(l, r) }

        Lifetime.using { lifetime ->
            runInParallelAsync(lifetime) {
                val job = coroutineContext.job
                var prevl: Int = -1
                var prevr: Int = -1
                var prev: Int = -1
                composeProperty.asFlow().collect {
                    if (it[0] == it[1]) {
                        if (prev != -1)
                            assertEquals(it[0] - 1, prev)

                        prev = it[0]
                        prevr = it[1]
                    } else {
                        assertEquals(it[0] - 1, it[1])

                    if (prevl != -1)
                        assertEquals(it[0] - 1, prevl)

                    if (prevr != -1)
                        assertEquals(it[1], prevr)

                        prevl = it[0]
                    }

                    if (it[0] == n && it[1] == n)
                        job.cancel()
                }
            }

            for (i in 0..1_000_000) {
                for (j in 0..n) {
                    optProperty1.set(j)
                    optProperty2.set(j)
                }

                optProperty1 = Property(0)
                optProperty2 = Property(0)

                composeProperty = optProperty1.compose(optProperty2) { l, r -> listOf(l, r) }
                yield()
            }

            for (i in 0..n) {
                optProperty1.set(i)
                optProperty2.set(i)
            }
        }
    }

    @Test
    fun composeOptPropertyCollectConcurrentTest() = runBlockingWithTimeout {
        val n = 5
        var optProperty1 = OptProperty<Int>()
        var optProperty2 = OptProperty<Int>()

        var composeProperty = optProperty1.compose(optProperty2) { l, r -> listOf(l, r) }

        Lifetime.using { lifetime ->
            runInParallelAsync(lifetime) { index ->
                val job = coroutineContext.job
                var prevl: Int = -1
                var prevr: Int = -1
                var prev: Int = -1
                composeProperty.asFlow().collect {
                    if (it[0] == it[1]) {
                        if (prev != -1)
                            assertEquals(it[0] - 1, prev)

                        prev = it[0]
                        prevr = it[1]
                    } else {
                        assertEquals(it[0] - 1, it[1])

                        if (prevl != -1)
                            assertEquals(it[0] - 1, prevl)

                        if (prevr != -1)
                            assertEquals(it[1], prevr)

                        prevl = it[0]
                    }

                    if (it[0] == n && it[1] == n)
                        job.cancel()
                }
            }

            for (i in 0..1_000_000) {
                for (j in 0..n) {
                    optProperty1.set(j)
                    optProperty2.set(j)
                }

                optProperty1 = OptProperty()
                optProperty2 = OptProperty()

                composeProperty = optProperty1.compose(optProperty2) { l, r -> listOf(l, r) }
                yield()
            }

            for (i in 0..n) {
                optProperty1.set(i)
                optProperty2.set(i)
            }
        }
    }

    private fun runBlockingWithTimeout(action: suspend CoroutineScope.() -> Unit) {
        runBlocking {
            for (i in 0 until 1) {
                withTimeout(1000.seconds) {
                    action()
                }
            }
        }
    }

    private fun CoroutineScope.runInParallelAsync(lifetime: Lifetime, action: suspend CoroutineScope.(Int) -> Unit) {
        launch(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) {
            for (i in 0 until 10) {
                launch(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) {
                    while (isActive && lifetime.isAlive) {
                        launch(start = CoroutineStart.UNDISPATCHED) {
                            action(i)
                        }.join()
                    }
                }
            }
        }
    }
}