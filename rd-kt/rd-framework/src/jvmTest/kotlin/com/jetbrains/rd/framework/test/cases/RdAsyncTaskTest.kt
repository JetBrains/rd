package com.jetbrains.rd.framework.test.cases

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.static
import com.jetbrains.rd.framework.impl.RdCall
import com.jetbrains.rd.framework.impl.RdOptionalProperty
import com.jetbrains.rd.framework.impl.RdSignal
import com.jetbrains.rd.framework.impl.RdTask
import com.jetbrains.rd.framework.test.util.RdFrameworkTestBase
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.lifetime.isAlive
import com.jetbrains.rd.util.lifetime.onTermination
import com.jetbrains.rd.util.reactive.hasValue
import com.jetbrains.rd.util.reactive.valueOrThrow
import com.jetbrains.rd.util.spinUntil
import com.jetbrains.rd.util.threading.Linearization
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
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

        server_property.set ( RdCall(null, Int::toString) )


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
        call2.set(null) { _ -> respSignal }

        serverProtocol.bindStatic(call1, "server")
        clientProtocol.bindStatic(call2, "client")


        val ld = LifetimeDefinition()
        val lf = ld.lifetime
        val task1 = call1.start(lf, Unit)

        spinUntil { task1.result.hasValue }
        com.jetbrains.rd.util.assert(task1.isSucceeded)

        val signal = task1.result.valueOrThrow.unwrap()
        val log = mutableListOf<Int>()
        signal.advise(Lifetime.Eternal) {
            log.add(it)
        }

        respSignal.fire(1)
        respSignal.fire(2)
        respSignal.fire(3)

        ld.terminate()
        respSignal.fire(4)

        spinUntil { log.count() >= 3 }
        Thread.sleep(100)
        log.toIntArray().contentEquals(arrayOf(1, 2, 3).toIntArray())
    }


}


