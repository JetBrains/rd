package com.jetbrains.rd.framework.test.cases

import com.jetbrains.rd.framework.base.static
import com.jetbrains.rd.framework.impl.RdSignal
import com.jetbrains.rd.framework.test.util.RdAsyncTestBase
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

class LateAdviseOrderingTest: RdAsyncTestBase() {

//    @Test // disabled: ordering on signals with late-ish adviseOn is not kept
    fun testLateAdviseOnCustomScheduler() {
        val serverSignal = RdSignal<Int>().static(1)
        val clientSignal = RdSignal<Int>().static(1)

        val latch1 = CountDownLatch(1)
        val latch2 = CountDownLatch(1)

        serverUiScheduler.queue {
            serverSignal.bind(serverLifetime, serverProtocol, "top")

            for (i in 0 until 10)
                serverSignal.fire(i)

            latch1.countDown()

            for (i in 10 until 10_000)
                serverSignal.fire(i)

            latch2.countDown()
        }

        val expectedValue = AtomicInteger(0)
        val observedOrder = ArrayList<Pair<Int, Boolean>>()

        clientUiScheduler.queue {
            clientSignal.bind(clientLifetime, clientProtocol, "top")

            latch1.await()

            clientSignal.adviseOn(clientLifetime, clientBgScheduler) {
                synchronized(observedOrder) { observedOrder.add(it to clientUiScheduler.isActive) }
                val expected = expectedValue.getAndIncrement()
                assert(it == expected) { "expected: $expected, got: $it" }

                if (it < 10)
                    assert(clientUiScheduler.isActive) { "First messages are expected to be processed on UI thread" }
                else
                    assert(clientBgScheduler.isActive) { "Last messages are expected to be processed on UI thread" }

                if (expected == 10)
                    Thread.sleep(500)
            }
        }

        latch2.await()

        serverUiScheduler.assertNoExceptions()
        serverBgScheduler.assertNoExceptions()

        clientUiScheduler.assertNoExceptions()
        clientBgScheduler.assertNoExceptions()

        assert(expectedValue.get() == 10_000) { "Not enough messages processed $expectedValue" }
    }
}