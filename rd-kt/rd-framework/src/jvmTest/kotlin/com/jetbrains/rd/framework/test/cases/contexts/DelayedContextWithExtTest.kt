package com.jetbrains.rd.framework.test.cases.contexts

import com.jetbrains.rd.framework.RdContext
import com.jetbrains.rd.framework.test.util.RdAsyncTestBase
import com.jetbrains.rd.util.threading.SpinWait
import demo.DemoModel
import demo.extModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.concurrent.CyclicBarrier

class DelayedContextWithExtTest : RdAsyncTestBase() {

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testExtNoFailureOnQueuedNewContextValue(useHeavyContext: Boolean) {
        println("useHeavyContext: $useHeavyContext")
        val context = if(useHeavyContext) ContextsTest.TestKeyHeavy else ContextsTest.TestKeyLight
        clientProtocol.serializers.register(RdContext.marshallerFor(context))

        setWireAutoFlush(true)

        val fireValues = listOf("a", "b", "c")

        val barrier0 = CyclicBarrier(2)
        val barrier1 = CyclicBarrier(2)
        val barrier2 = CyclicBarrier(2)

        serverUiScheduler.queue {
            serverProtocol.contexts.registerContext(context)
            val serverModel = DemoModel.create(serverLifetime, serverProtocol)

            barrier0.await() // root model also uses ext semantics, so make sure both ends have created it and processed its connection message

            val serverExt = serverModel.extModel

            fireValues.forEach {
                context.value = it
                serverExt.checker.fire(Unit)
                context.value = null
            }

            barrier1.await()
        }

        var numReceives = 0
        val receivedContexts = mutableSetOf<String>()

        clientUiScheduler.queue {
            val clientModel = DemoModel.create(clientLifetime, clientProtocol)

            barrier0.await()
            barrier1.await()

            serverUiScheduler.queue {
                barrier2.await()
            }
            barrier2.await()

            val clientExt = clientModel.extModel

            Thread.sleep(500)

            clientExt.checker.advise(clientLifetime) {
                numReceives++
                receivedContexts.add(context.value ?: "null")
            }
        }

        SpinWait.spinUntil(5_000) { numReceives == 3 }

        assertEquals(3, numReceives)
        assertEquals(fireValues.toSet(), receivedContexts)
    }
}