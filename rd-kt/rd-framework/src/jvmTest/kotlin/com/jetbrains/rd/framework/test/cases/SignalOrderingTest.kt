package com.jetbrains.rd.framework.test.cases

import com.jetbrains.rd.framework.FrameworkMarshallers
import com.jetbrains.rd.framework.base.static
import com.jetbrains.rd.framework.impl.RdSignal
import com.jetbrains.rd.framework.test.util.RdAsyncTestBase
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch

class SignalOrderingTest : RdAsyncTestBase() {

    @Test
    fun testSignalOrderingWithLateBind() {
        val evt1 = CountDownLatch(3)
        val evt2 = CountDownLatch(1)

        val iterationCount = 10_000

        clientUiScheduler.queue {
            val clientSignal1 = clientProtocol.bindStatic(RdSignal(FrameworkMarshallers.String).static(1), "top1")
            val clientSignal2 = clientProtocol.bindStatic(RdSignal(FrameworkMarshallers.String).static(2), "top2")

            evt1.countDown()
            evt1.await()

            clientUiScheduler.queue {
                evt2.await()
                for (i in 0 until iterationCount) {
                    clientSignal1.fire("a$i")
                    clientSignal2.fire("b$i")
                }
            }
        }

        var counter = 0
        var awaiting1 = true

        serverUiScheduler.queue {
            evt1.countDown()
            evt1.await()

            evt2.countDown()

            val serverSignal1 = RdSignal(FrameworkMarshallers.String).static(1)
            val serverSignal2 = RdSignal(FrameworkMarshallers.String).static(2)

            serverSignal1.adviseOn(serverLifetime, serverUiScheduler) {
                assert(awaiting1) { "did not expect signal 1 to fire with $it" }
                assert(it.substring(1).toInt() == counter) { "out-of-order receive in signal? $it" }
                awaiting1 = false
            }

            serverSignal2.adviseOn(serverLifetime, serverUiScheduler) {
                assert(!awaiting1) { "did not expect signal 2 to fire with $it" }
                assert(it.substring(1).toInt() == counter) { "out-of-order receive in signal? $it" }
                awaiting1 = true
                counter++
            }

            Thread.sleep(1_000)

            serverProtocol.bindStatic(serverSignal1, "top1")
            serverProtocol.bindStatic(serverSignal2, "top2")
        }

        evt1.countDown()
        evt1.await()

        // these also flush the schedulers
        clientUiScheduler.assertNoExceptions()
        serverUiScheduler.assertNoExceptions()

        assert(counter == iterationCount) { "Some values were not received" }
    }
}