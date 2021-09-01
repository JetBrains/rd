package com.jetbrains.rd.framework.test.cases

import com.jetbrains.rd.framework.base.static
import com.jetbrains.rd.framework.impl.RdSignal
import com.jetbrains.rd.framework.test.util.RdAsyncTestBase
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.fire
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

class RdAsyncSignalTest : RdAsyncTestBase() {

    @Test
    fun testAsyncSignalStatic() {

        val acc = AtomicInteger(0)

        val evt1 = CountDownLatch(3)
        val evt2 = CountDownLatch(1)

        clientUiScheduler.queue {
            val clientSignal = clientProtocol.bindStatic(RdSignal<Unit>().static(1).apply { async = true }, "top")

            evt1.countDown()
            evt1.await()
            clientBgScheduler.queue {
                clientSignal.fire()
                println("client fired bg")
            }
            clientSignal.fire()
            println("client fired ui")
            evt2.countDown()
        }

        serverUiScheduler.queue {
            val serverSignal = serverProtocol.bindStatic(RdSignal<Unit>().static(1).apply { async = true }, "top")
            Lifetime.using {
                serverSignal.adviseOn(serverLifetime, serverBgScheduler) {
                    println("server received")
                    Thread.sleep(100)
                    serverBgScheduler.assertThread()
                    acc.incrementAndGet()
                }
                println("server advise completed")
                evt1.countDown()
                evt1.await()

            }
        }

        assertEquals(0, acc.get())
        evt1.countDown()
        evt1.await()

        evt2.await()
        clientUiScheduler.assertNoExceptions()
        clientBgScheduler.assertNoExceptions()


        serverUiScheduler.assertNoExceptions()
        serverBgScheduler.assertNoExceptions()

        assertEquals(2, acc.get())
        println("end of all things")
    }
}