package com.jetbrains.rd.framework.test.cases.wire

import com.jetbrains.rd.framework.base.static
import com.jetbrains.rd.framework.impl.RdSignal
import com.jetbrains.rd.framework.test.util.TestBase
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.threading.SpinWait
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals


class SocketProxyTest : TestBase() {
    private val DefaultTimeoutMs = 100L

    @Test
    fun testSimple() {
        // using val factory = Log.UsingLogFactory( TextWriterLogFactory(Console.Out, LoggingLevel.TRACE))
        Lifetime.using { lifetime ->
            val proxyLifetimeDefinition = lifetime.createNested()
            val proxyLifetime = proxyLifetimeDefinition.lifetime
            {

                val serverProtocol = SocketWireTest.server(lifetime)

                val proxy = SocketProxy("TestProxy", proxyLifetime, serverProtocol).apply { start() }
                Thread.sleep(DefaultTimeoutMs)

                val clientProtocol = SocketWireTest.client(lifetime, proxy.port)

                val sp = RdSignal<Int>().static(1)
                sp.bind(lifetime, serverProtocol, SocketWireTest.top)

                val cp = RdSignal<Int>().static(1)
                cp.bind(lifetime, clientProtocol, SocketWireTest.top)

                val serverLog = mutableListOf<Int>()
                val clientLog = mutableListOf<Int>()

                sp.advise(lifetime) { i -> serverLog.add(i) }
                cp.advise(lifetime) { i -> clientLog.add(i) }

                //Connection is established for now

                sp.fire(1)
                Thread.sleep(DefaultTimeoutMs)
                cp.fire(2)

                SpinWait.spinUntil { serverLog.size == 2 }
                SpinWait.spinUntil { clientLog.size == 2 }
                assertEquals(listOf(1, 2), serverLog)
                assertEquals(listOf(1, 2), clientLog)


                proxy.stopServerToClientMessaging()

                sp.fire(3)
                Thread.sleep(DefaultTimeoutMs)
                assertEquals(listOf(1, 2, 3), serverLog)
                assertEquals(listOf(1, 2), clientLog)


                proxy.stopClientToServerMessaging()

                cp.fire(4)
                Thread.sleep(DefaultTimeoutMs)
                assertEquals(listOf(1, 2, 3), serverLog)
                assertEquals(listOf(1, 2, 4), clientLog)

                //Connection is broken for now

                proxy.startServerToClientMessaging()

                sp.fire(5)
                SpinWait.spinUntil { serverLog.size == 4 }
                SpinWait.spinUntil { clientLog.size == 4 }
                assertEquals(listOf(1, 2, 3, 5), serverLog)
                assertEquals(listOf(1, 2, 4, 5), clientLog)


                proxy.startClientToServerMessaging()

                cp.fire(6)
                SpinWait.spinUntil { serverLog.size == 5 }
                SpinWait.spinUntil { clientLog.size == 5 }
                assertEquals(listOf(1, 2, 3, 5, 6), serverLog)
                assertEquals(listOf(1, 2, 4, 5, 6), clientLog)

                //Connection is established for now

                proxyLifetimeDefinition.terminate()

                sp.fire(7)
                Thread.sleep(DefaultTimeoutMs)
                cp.fire(8)

                SpinWait.spinUntil { serverLog.size == 6 }
                SpinWait.spinUntil { clientLog.size == 6 }
                assertEquals(listOf(1, 2, 3, 5, 6, 7), serverLog)
                assertEquals(listOf(1, 2, 4, 5, 6, 8), clientLog)

                //Connection is broken for now, proxy is not alive
            }
        }
    }
}
