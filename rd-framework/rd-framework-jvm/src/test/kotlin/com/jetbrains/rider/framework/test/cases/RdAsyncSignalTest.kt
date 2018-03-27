package com.jetbrains.rider.framework.test.cases

import com.jetbrains.rider.framework.base.static
import com.jetbrains.rider.framework.impl.RdSignal
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.reactive.fire
import org.testng.annotations.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals

class RdAsyncSignalTest : RdAsyncTestBase() {

    @Test
    fun TestAsyncSignalStatic() {

        val acc = AtomicInteger(0)

        val evt1 = CountDownLatch(3)
        val evt2 = CountDownLatch(1)

        clientUiScheduler.queue {
            val client_signal = clientProtocol.bindStatic(RdSignal<Unit>().static(1).apply { async = true }, "top")

            evt1.countDown()
            evt1.await()
            clientBgScheduler.queue {
                client_signal.fire()
                println("client fired bg")
            }
            client_signal.fire()
            println("client fired ui")
            evt2.countDown()
        }

        serverUiScheduler.queue {
            val server_signal = serverProtocol.bindStatic(RdSignal<Unit>().static(1).apply { async = true }, "top")
            Lifetime.using { _ ->
                server_signal.adviseOn(serverLifetime, serverBgScheduler, {
                    println("server received")
                    Thread.sleep(100)
                    serverBgScheduler.assertThread()
                    acc.incrementAndGet()
                })
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