package com.jetbrains.rd.framework.test.cases.contexts

import com.jetbrains.rd.framework.FrameworkMarshallers
import com.jetbrains.rd.framework.RdContext
import com.jetbrains.rd.framework.impl.RdSignal
import com.jetbrains.rd.framework.test.util.RdAsyncTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch

class ContextsMultithreadTest : RdAsyncTestBase() {
    @Test
    fun testBasic() {
        val key = object : RdContext<String>("test-key", false, FrameworkMarshallers.String) {}

        val serverSignal = RdSignal<String>().also { it.async = true }
        val clientSignal = RdSignal<String>()

        val latch1 = CountDownLatch(4)
        val latch2 = CountDownLatch(2)

        val values = (1..100_000).map { it.toString() }

        serverUiScheduler.queue {
            serverProtocol.bindStatic(serverSignal, 1)
            serverProtocol.contexts.registerContext(key)

            serverBgScheduler.queue {
                latch1.countDown()
                latch1.await()

                values.asReversed().forEach {
                    key.value = it
                    serverSignal.fire(it)
                }

                latch2.countDown()
            }

            latch1.countDown()
            latch1.await()

            values.forEach {
                key.value = it
                serverSignal.fire(it)
            }

            latch2.countDown()
        }

        clientUiScheduler.queue {
            clientProtocol.bindStatic(clientSignal, 1)

            clientSignal.advise(clientLifetime) {
                assertEquals(it, key.value)
            }

            latch1.countDown()
            latch1.await()
        }

        latch1.countDown()

        latch2.await()


        clientUiScheduler.assertNoExceptions()
        clientBgScheduler.assertNoExceptions()

        serverUiScheduler.assertNoExceptions()
        serverBgScheduler.assertNoExceptions()
    }
}