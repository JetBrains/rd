package com.jetbrains.rd.framework.test.cases.wire

import com.jetbrains.rd.framework.base.static
import com.jetbrains.rd.framework.impl.RdSignal
import com.jetbrains.rd.framework.test.util.TestBase
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.threading.SpinWait
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class SocketProxyTest : TestBase() {
    private val DefaultTimeoutMs = 100L

    @Test
    @Disabled("Unstable")
    fun testSimple() {
        // using val factory = Log.UsingLogFactory( TextWriterLogFactory(Console.Out, LoggingLevel.TRACE))
        Lifetime.using { lifetime ->
            val proxyLifetimeDefinition = lifetime.createNested()
            val proxyLifetime = proxyLifetimeDefinition.lifetime

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
            SpinWait.spinUntil { serverLog.size == 1 }
            SpinWait.spinUntil { clientLog.size == 1 }
            assertEquals(listOf(1), serverLog)
            assertEquals(listOf(1), clientLog)


            cp.fire(2)
            SpinWait.spinUntil { serverLog.size == 2 }
            SpinWait.spinUntil { clientLog.size == 2 }
            assertEquals(listOf(1, 2), serverLog)
            assertEquals(listOf(1, 2), clientLog)


            proxy.stopServerToClientMessaging()

            cp.advise(lifetime) { i -> assertNotEquals(3, i, "Value 3 mustn't be received") }

            sp.fire(3)
            SpinWait.spinUntil { serverLog.size == 3 }
            assertEquals(listOf(1, 2, 3), serverLog)


            proxy.stopClientToServerMessaging()

            sp.advise(lifetime) { i -> assertNotEquals(4, i, "Value 4 mustn't be received") }

            cp.fire(4)
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

            cp.advise(lifetime) { i -> assertNotEquals(7, i, "Value 7 mustn't be received") }
            sp.fire(7)

            SpinWait.spinUntil { serverLog.size == 6 }
            assertEquals(listOf(1, 2, 3, 5, 6, 7), serverLog)


            sp.advise(lifetime) { i -> assertNotEquals(8, i, "Value 8 mustn't be received") }
            cp.fire(8)

            SpinWait.spinUntil { clientLog.size == 6 }
            assertEquals(listOf(1, 2, 4, 5, 6, 8), clientLog)

            //Connection is broken for now, proxy is not alive
        }
    }
}
