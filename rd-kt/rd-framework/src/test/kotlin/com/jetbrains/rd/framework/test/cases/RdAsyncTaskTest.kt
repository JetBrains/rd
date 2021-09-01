package com.jetbrains.rd.framework.test.cases

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.static
import com.jetbrains.rd.framework.impl.RdCall
import com.jetbrains.rd.framework.impl.RdOptionalProperty
import com.jetbrains.rd.framework.impl.RdSignal
import com.jetbrains.rd.framework.impl.RdTask
import com.jetbrains.rd.framework.test.util.RdFrameworkTestBase
import com.jetbrains.rd.framework.util.*
import com.jetbrains.rd.util.AtomicInteger
import com.jetbrains.rd.util.error
import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.lifetime.*
import com.jetbrains.rd.util.reactive.RdFault
import com.jetbrains.rd.util.reactive.hasValue
import com.jetbrains.rd.util.reactive.valueOrThrow
import com.jetbrains.rd.util.spinUntil
import com.jetbrains.rd.util.threading.Linearization
import com.jetbrains.rd.util.threading.SingleThreadScheduler
import com.jetbrains.rd.util.threading.TestSingleThreadScheduler
import kotlinx.coroutines.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class RdAsyncTaskTest : RdFrameworkTestBase() {
    @Suppress("UNCHECKED_CAST")
    @Test
    fun TestDynamic() {
        val property_id = 1
        val client_property = RdOptionalProperty(RdCall as ISerializer<RdCall<Int, String>>).static(property_id)
        val server_property = RdOptionalProperty(RdCall.Companion as ISerializer<RdCall<Int, String>>).static(property_id).slave()


        //bound
        clientProtocol.bindStatic(client_property, "top")
        serverProtocol.bindStatic(server_property, "top")

        server_property.set ( RdCall(null, null, Int::toString) )


        assertEquals("1", client_property.valueOrThrow.sync(1))

        val l = Linearization()
        server_property.set (
                RdCall<Int, String> { _, v ->
                RdTask<String>().apply {
                    thread {
                        l.point(1)
                        this.set(v.toString())
                        l.point(2)
                    }
                }
            }
        )

        l.point(0)
        assertEquals("2", client_property.valueOrThrow.sync(2))
        l.point(3)
        l.reset()


        //wait for task
        val task = client_property.valueOrThrow.start(3)
        assertFalse(task.isSucceeded)
        assertFalse(task.isCanceled)
        assertFalse(task.isFaulted)
        assertFalse(task.result.hasValue)
        l.point(0)

        l.point(3)
        assertTrue(task.isSucceeded, "${task.result.hasValue}")
        assertFalse(task.isCanceled)
        assertFalse(task.isFaulted)
        assertTrue(task.result.hasValue)

        l.reset()
        val interruptedTask = client_property.valueOrThrow.start(0)

        //terminate request
        client_property.set(RdCall())

        assertTrue { interruptedTask.isCanceled }
        l.disable()

        when (interruptedTask.result.valueOrThrow) {
            is RdTaskResult.Success<*> -> assertThrows(Throwable::class.java) {  }
            is RdTaskResult.Cancelled<*> -> {}
            is RdTaskResult.Fault<*> -> assertThrows(Throwable::class.java) {  }
            else -> assertThrows(Throwable::class.java) {  }
        }
    }

    @Test
    fun testCancellation() {
        val entity_id = 1

        val server_entity = RdCall<Unit, String>().static(entity_id)
        val client_entity = RdCall<Unit, String>(null) { x -> x.toString()}.static(entity_id)

        clientProtocol.bindStatic(client_entity, "client")
        serverProtocol.bindStatic(server_entity, "server")

        var handlerFinished = false
        var handlerCompletedSuccessfully = false
        client_entity.set(null) { lf, req ->
            val rdTask = RdTask<String>()
            val syncPoint = CountDownLatch(1)
            val job = GlobalScope.launch {
                try {
                    syncPoint.countDown()
                    delay(500)
                    if (lf.isAlive)
                        handlerCompletedSuccessfully = true
                } finally {
                    handlerFinished = true
                }
                rdTask.set("")
            }

            lf.onTermination { syncPoint.await(); job.cancel() }
            return@set rdTask
        }

        //1. explicit cancellation
        val ld = LifetimeDefinition()
        var task = server_entity.start(ld.lifetime, Unit)
        ld.terminate()

        spinUntil { task.result.hasValue }
        assert(task.isCanceled)

        spinUntil { handlerFinished }
        assertFalse(handlerCompletedSuccessfully)

        //2. no cancellation
        handlerFinished = false
        handlerCompletedSuccessfully = false
        task = server_entity.start(LifetimeDefinition().lifetime, Unit)

        spinUntil { task.result.hasValue }
        assert(task.isSucceeded)

        spinUntil { handlerFinished }
        assert(handlerCompletedSuccessfully)

        //3. cancellation from parent lifetime
        handlerFinished = false
        handlerCompletedSuccessfully = false
        clientLifetime
        task = server_entity.start(LifetimeDefinition().lifetime, Unit)
        clientLifetimeDef.terminate()
        serverLifetimeDef.terminate()

        spinUntil { task.result.hasValue }
        assert(task.isCanceled)

        spinUntil { handlerFinished }
        assertFalse(handlerCompletedSuccessfully)
    }

    @Test
    fun testBindable() {
        val entity_id = 1

        val call1 = RdCall(FrameworkMarshallers.Void, RdSignal.Companion as ISerializer<RdSignal<Int>>).static(entity_id)
        val call2 = RdCall(FrameworkMarshallers.Void, RdSignal.Companion as ISerializer<RdSignal<Int>>).static(entity_id)

        val respSignal = RdSignal<Int>()
        var endpointLfTerminated = false
        call2.set(null) { endpointLf, _ ->
            endpointLf.onTermination { endpointLfTerminated = true }
            RdTask.fromResult(respSignal)
        }

        serverProtocol.bindStatic(call1, "server")
        clientProtocol.bindStatic(call2, "client")


        val ld = LifetimeDefinition()
        val lf = ld.lifetime
        val task1 = call1.start(lf, Unit)

        spinUntil { task1.result.hasValue }
        assert(task1.isSucceeded)

        val signal = task1.result.valueOrThrow.unwrap()
        val log = mutableListOf<Int>()
        signal.advise(Lifetime.Eternal) {
            log.add(it)
        }

        respSignal.fire(1)
        respSignal.fire(2)
        respSignal.fire(3)

        ld.terminate()
        assertFalse(respSignal.isBound)

        spinUntil { log.count() >= 3 }
        Thread.sleep(100)
        log.toIntArray().contentEquals(arrayOf(1, 2, 3).toIntArray())

        assert(endpointLfTerminated)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun testToRdTask() {
        val entity_id = 1

        val server_entity = RdCall<Unit, String>().static(entity_id)
        val client_entity = RdCall<Unit, String>(null) { x -> x.toString() }.static(entity_id)

        clientProtocol.bindStatic(client_entity, "client")
        serverProtocol.bindStatic(server_entity, "server")

        client_entity.set(null) { _, _ -> GlobalScope.async { "0" }.toRdTask() }

        Lifetime.using { lf ->
            val task = server_entity.start(lf, Unit)
            task.wait(500) { task.isSucceeded }
            assert(task.isSucceeded)
            assertEquals("0", task.result.valueOrThrow.unwrap())
        }

        client_entity.set(null) { lf, req -> GlobalScope.async { throw CancellationException() }.toRdTask() }

        Lifetime.using { lf ->
            val task = server_entity.start(lf, Unit)
            task.wait(500) { task.isCanceled }
            assert(task.isCanceled)
        }

        client_entity.set(null) { lf, req -> GlobalScope.async { throw IllegalArgumentException("1") }.toRdTask() }

        Lifetime.using { lf ->
            val task = server_entity.start(lf, Unit)
            task.wait(500) { task.isFaulted }
            assert(task.isFaulted)
            val res = task.result.valueOrThrow
            assertEquals("1", (res as RdTaskResult.Fault).error.reasonMessage)
        }
    }

    @Test
    fun testAsCompletableFuture() {
        val entity_id = 1

        val server_entity = RdCall<Unit, String>().static(entity_id)
        val client_entity = RdCall<Unit, String>(null) { x -> x.toString() }.static(entity_id)

        clientProtocol.bindStatic(client_entity, "client")
        serverProtocol.bindStatic(server_entity, "server")

        client_entity.set(null) { _ -> "0" }

        Lifetime.using { lf ->
            val future = server_entity.start(lf, Unit).asCompletableFuture()
            assert(future.isDone)
            assertEquals("0", future.getNow(null))
        }

        client_entity.set(null) { _, _ -> RdTask.canceled() }

        Lifetime.using { lf ->
            val future = server_entity.start(lf, Unit).asCompletableFuture()
            assert(future.isCancelled)
        }

        client_entity.set(null) { _ -> throw IllegalArgumentException("1") }

        Lifetime.using { lf ->
            val future = server_entity.start(lf, Unit).asCompletableFuture()
            assert(future.isCompletedExceptionally)
        }
    }

    @Test
    fun testCompletableFutureToRdTask() {
        val entity_id = 1

        val server_entity = RdCall<Unit, String>().static(entity_id)
        val client_entity = RdCall<Unit, String>(null) { x -> x.toString() }.static(entity_id)

        clientProtocol.bindStatic(client_entity, "client")
        serverProtocol.bindStatic(server_entity, "server")

        client_entity.set(null) { _ -> "0" }

        Lifetime.using { lf ->
            val rdTask = server_entity.start(lf, Unit).asCompletableFuture().toRdTask()
            assert(rdTask.isSucceeded)
            assertEquals("0", rdTask.result.valueOrThrow.unwrap())
        }

        client_entity.set(null) { _, _ -> RdTask.canceled() }

        Lifetime.using { lf ->
            val future = server_entity.start(lf, Unit).asCompletableFuture().toRdTask()
            assert(future.isCanceled)
        }

        client_entity.set(null) { _ -> throw IllegalArgumentException("1") }

        Lifetime.using { lf ->
            val future = server_entity.start(lf, Unit).asCompletableFuture().toRdTask()
            assert(future.isFaulted)
        }
    }

    @Test
    fun testRdTaskAwait() {
        val entity_id = 1

        val server_entity = RdCall<Unit, String>().static(entity_id)
        val client_entity = RdCall<Unit, String>(null) { x -> x.toString() }.static(entity_id)

        clientProtocol.bindStatic(client_entity, "client")
        serverProtocol.bindStatic(server_entity, "server")

        client_entity.set(null) { _ -> "0" }

        var completedSuccessfully = false
        var error: Throwable? = null

        Lifetime.using { lf ->
            val job = GlobalScope.launch {
                try {
                    val res = server_entity.start(lf, Unit).await()
                    assertEquals("0", res)
                    completedSuccessfully = true
                } catch (e: Throwable) {
                    error = e
                }
            }
            spinUntil { job.isCompleted }
            assertNull(error)
            assert(completedSuccessfully)
        }

        client_entity.set(null) { _, _ -> RdTask.canceled() }

        completedSuccessfully = false
        error = null

        Lifetime.using { lf ->
            val job = GlobalScope.launch {
                try {
                    val res = server_entity.start(lf, Unit).await()
                    assertEquals("0", res)
                    completedSuccessfully = true
                } catch (e: Throwable) {
                    error = e
                }
            }
            spinUntil { job.isCompleted }
            assert(error is CancellationException)
        }

        client_entity.set(null) { _, _ -> throw IllegalArgumentException("1") }

        completedSuccessfully = false
        error = null

        Lifetime.using { lf ->
            val job = GlobalScope.launch {
                try {
                    val res = server_entity.start(lf, Unit).await()
                    assertEquals("0", res)
                    completedSuccessfully = true
                } catch (e: Throwable) {
                    error = e
                }
            }
            spinUntil { job.isCompleted }
            assertNotNull(error)
        }
    }

    @ExperimentalCoroutinesApi
    @Test
    fun testRdTaskAwaitAll() {
        val entity_id = 1

        val server_entity = RdCall<Long, Long>().static(entity_id)
        val client_entity = RdCall<Long, Long>(null) { x -> x ?: 0 }.static(entity_id)

        clientProtocol.bindStatic(client_entity, "client")
        serverProtocol.bindStatic(server_entity, "server")


        var completedSuccessfully = false
        var error: Throwable? = null
        val count = AtomicInteger(0)

        client_entity.set(null) { _, req ->
            GlobalScope.async {
                if  (req < 0)
                    throw KotlinNullPointerException(req.toString())

                delay(req * 10)
                count.incrementAndGet()
                req
            }.toRdTask()
        }

        Lifetime.using { lf ->
            val job = GlobalScope.launch {
                try {
                    val list = listOf<Long>(10, 20, 5, 30, 40)
                    val result = list.map { server_entity.start(lf, it) }.awaitAll()

                    assertEquals(list.size, result.size)
                    list.forEachIndexed { index, value ->
                        assertEquals(value, result[index])
                    }
                    assertEquals(list.size, count.get())
                    completedSuccessfully = true
                } catch (e: Throwable) {
                    error = e
                }
            }
            spinUntil { job.isCompleted }
            assertNull(error)
            assert(completedSuccessfully)
        }

        completedSuccessfully = false
        error = null
        count.set(0)

        Lifetime.using { lf ->
            val list = listOf<Long>(10, 20, 5, 30, 40, -1)
            val job = GlobalScope.launch {
                try {
                    list.map { server_entity.start(lf, it) }.awaitAll()
                    completedSuccessfully = true
                } catch (e: Throwable) {
                    error = e
                }
            }
            spinUntil { job.isCompleted }
            assertEquals(list.size - 1, count.get())
            assertNotNull(error)
            assertFalse(completedSuccessfully)
        }
    }

    @Test
    fun testOverriddenHandlerScheduler() {
        val entity_id = 1

        val callsite = RdCall<Int, String>().static(entity_id)
        val endpoint = RdCall<Int, String>(null) { x -> throw Exception() }.static(entity_id)

        clientProtocol.bindStatic(callsite, "client")
        serverProtocol.bindStatic(endpoint, "server")


        val scheduler = TestSingleThreadScheduler("Test background scheduler")
        assertFalse(scheduler.isActive);

        val point1 = CountDownLatch(1)
        val point2 = CountDownLatch(1)

        endpoint.set(handlerScheduler = scheduler) { _, req ->
            assertTrue(scheduler.isActive)
            point1.countDown()

            point2.await(10, TimeUnit.SECONDS)
            assertEquals(0L, point2.count)
            return@set RdTask.fromResult(req.toString())
        }

        assertFalse(scheduler.isActive)

        val task = callsite.start(0);
        val result = task.result;

        assertFalse(result.hasValue)

        point1.await(10, TimeUnit.SECONDS)
        assertEquals(0L, point1.count)

        assertFalse(result.hasValue)
        point2.countDown()

        spinUntil(10000) { result.hasValue }
        assertTrue(result.hasValue)

        assertEquals("0", result.valueOrThrow.unwrap())
    }

    @Test
    fun startSuspendingTest() {
        val entity_id = 1

        val callsite = RdCall<Int, String>().static(entity_id)
        val endpoint = RdCall<Int, String>(null) { x -> throw Exception() }.static(entity_id)

        clientProtocol.bindStatic(callsite, "client")
        serverProtocol.bindStatic(endpoint, "server")


        Lifetime.using { lifetime ->
            val scheduler = SingleThreadScheduler(lifetime, "TestScheduler")

            // send receive
            endpoint.set { _, req -> lifetime.startAsync(scheduler) { req.toString() }.toRdTask() }

            runBlocking(scheduler.asCoroutineDispatcher) {
                var res = callsite.startSuspending(Lifetime.Eternal, 0)
                assertEquals("0", res)

                res = callsite.startSuspending(Lifetime.Eternal, 1)
                assertEquals("1", res)
            }


            // auto cancellation
            var cancelled: Boolean? = null
            endpoint.set { lf, req ->
                lifetime.startAsync(scheduler) {
                    lf.waitFor(Duration.ofSeconds(10)) { false } //
                    cancelled = lf.isNotAlive
                    assert(lf.isNotAlive)
                    req.toString()
                }.toRdTask()
            }

            runBlocking(scheduler.asCoroutineDispatcher) {
                val job = launch {
                    callsite.startSuspending(Lifetime.Eternal, 0)
                }

                yield() // wait for startSuspending asynchronously

                job.cancel()
                lifetime.waitFor(Duration.ofSeconds(1)) { cancelled != null } //non blocking wait
                assertNotEquals(null, cancelled) { "cancelled must be initialized" }
                assert(cancelled!!) { "cancelled must be true" }
            }
        }
    }

    @Test
    fun setSuspendNotBoundTest() {
        val entity_id = 1

        val callsite = RdCall<Int, String>().static(entity_id)
        val endpoint = RdCall<Int, String>(null) { x -> throw Exception() }.static(entity_id)

        endpoint.setSuspend { lifetime, arg ->
            delay(1)
            arg.toString()
        }

        clientProtocol.bindStatic(callsite, "client")
        serverProtocol.bindStatic(endpoint, "server")

        runBlocking {
            withTimeout(1000) {
                val  result = callsite.startSuspending(Lifetime.Eternal, 1)
                assertEquals("1", result)
            }
        }
    }

    @Test
    fun setSuspendTest() {
        val entity_id = 1

        val callsite = RdCall<Int, String>().static(entity_id)
        val endpoint = RdCall<Int, String>(null) { x -> throw Exception() }.static(entity_id)

        clientProtocol.bindStatic(callsite, "client")
        serverProtocol.bindStatic(endpoint, "server")

        Lifetime.using { lifetime ->
            val scheduler = SingleThreadScheduler(lifetime, "TestScheduler")

            var count = 0
            val cancelled = AtomicBoolean(false)
            endpoint.setSuspend(handlerScheduler = scheduler) { lf, arg ->
                scheduler.assertThread()
                return@setSuspend when (arg) {
                    1 -> {
                        assertEquals(0, count)
                        count = 1
                        "1"
                    }
                    2 -> {
                        assertEquals(2, count)
                        delay(10)
                        return@setSuspend "2"
                    }
                    3 -> {
                        assertEquals(2, count)
                        count = 3
                        try {
                            lf.waitFor(Duration.ofSeconds(10)) { false }
                        } catch (e: CancellationException) {
                            cancelled.set(true)
                            throw e;
                        }

                        getLogger<RdTaskTest>().error { "Must not be reached" }
                        return@setSuspend "3"
                    }
                    else -> {
                        throw TestException()
                    }
                }
            }

            runBlocking {
                val task = callsite.start(lifetime, 1)

                scheduler.queue {
                    assertEquals(1, count)
                    count = 2
                }

                assertEquals("1", task.awaitInternal())

                assertEquals("2", callsite.startSuspending(lifetime, 2))

                lifetime.usingNested { nestedLifetime ->
                    callsite.start(nestedLifetime, 3)
                    withContext(scheduler) { assertEquals(3, count) } // sync with endpoint
                }

                lifetime.waitFor(Duration.ofSeconds(1)) { cancelled.get() }
                assert(cancelled.get())

                try {
                    callsite.startSuspending(lifetime, 4)
                    getLogger<RdTaskTest>().error { "Must not be reached" }
                } catch (e: RdFault) {
                    assert(e.reasonTypeFqn == "TestException")
                    // ok
                }
            }
        }
    }

    private class TestException : Exception()
}


