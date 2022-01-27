package com.jetbrains.rd.framework.test.cases

import com.jetbrains.rd.framework.FrameworkMarshallers
import com.jetbrains.rd.framework.ISerializer
import com.jetbrains.rd.framework.base.static
import com.jetbrains.rd.framework.impl.RdCall
import com.jetbrains.rd.framework.impl.RdSignal
import com.jetbrains.rd.framework.test.util.RdFrameworkTestBase
import com.jetbrains.rd.framework.util.asCoroutineDispatcher
import com.jetbrains.rd.framework.util.setSuspend
import com.jetbrains.rd.framework.util.withContext
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.threading.TestSingleThreadScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RdAsyncTaskTest2 : RdFrameworkTestBase() {

    override val clientScheduler: IScheduler = TestSingleThreadScheduler("Client Protocol Scheduler")
    override val serverScheduler: IScheduler = TestSingleThreadScheduler("Server Protocol Scheduler")

    @Test
    fun defaultResponseScheduler() {
        val entity_id = 1

        val callsite = RdCall<Int, String>().static(entity_id).apply { async = true }
        val endpoint = RdCall<Int, String>(null) { x -> throw Exception() }.static(entity_id)

        runBlocking(clientScheduler.asCoroutineDispatcher) {
            clientProtocol.bindStatic(callsite, "client")
        }

        runBlocking(serverScheduler.asCoroutineDispatcher) {
            serverProtocol.bindStatic(endpoint, "server")
        }

        endpoint.setSuspend { lifetime, arg ->
            serverScheduler.assertThread()
            yield()
            serverScheduler.assertThread()
            arg.toString()
        }


        runBlocking(clientScheduler.asCoroutineDispatcher) {
            runBlocking {
                withTimeout(5000L) {
                    val result = callsite.startSuspending(5)
                    Assertions.assertEquals("5", result)
                }
            }
        }
    }

    @Test
    fun defaultResponseSchedulerWithBindableResult() {
        val entity_id = 1

        val callsite = RdCall(FrameworkMarshallers.Void, RdSignal.Companion as ISerializer<RdSignal<Int>>).static(entity_id).apply { async = true }
        val endpoint = RdCall(FrameworkMarshallers.Void, RdSignal.Companion as ISerializer<RdSignal<Int>>).static(entity_id)

        runBlocking(clientScheduler.asCoroutineDispatcher) {
            clientProtocol.bindStatic(callsite, "client")
        }

        runBlocking(serverScheduler.asCoroutineDispatcher) {
            serverProtocol.bindStatic(endpoint, "server")
        }

        val signal = RdSignal<Int>()

        val list = mutableListOf<Int>()

        Lifetime.using { adviseLifetime ->

            signal.advise(adviseLifetime) { value ->
                serverScheduler.assertThread()
                list.add(value)
            }

            endpoint.setSuspend { lifetime, arg ->
                serverScheduler.assertThread()
                yield()
                serverScheduler.assertThread()
                signal
            }


            runBlocking(clientScheduler.asCoroutineDispatcher) {
                val def = clientLifetime.createNested()
                val lifetime = def.lifetime

                val result = runBlocking {
                    withTimeout(5000L) {
                        callsite.startSuspending(lifetime, Unit)
                    }
                }

                result.fire(1)
                withContext(serverScheduler) {
                    assertEquals(1, list.single())
                }

                result.fire(2)
                withContext(serverScheduler) {
                    assertEquals(1, list[0])
                    assertEquals(2, list[1])
                    assertEquals(2, list.size)
                }

                def.terminate()
                result.fire(3)
                withContext(serverScheduler) {
                    assertEquals(1, list[0])
                    assertEquals(2, list[1])
                    assertEquals(2, list.size)
                }
            }
        }
    }
}