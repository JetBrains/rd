package com.jetbrains.rd.framework.test.cases.wire

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.static
import com.jetbrains.rd.framework.impl.RdOptionalProperty
import com.jetbrains.rd.framework.impl.RdSignal
import com.jetbrains.rd.framework.test.util.TestBase
import com.jetbrains.rd.framework.test.util.TestScheduler
import com.jetbrains.rd.framework.util.NetUtils
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.spinUntil
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.net.InetAddress
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeoutException


class SocketWireTest : TestBase() {

    private fun <T : Any> RdOptionalProperty<T>.waitAndAssert(expected: T, prev: T? = null) {
        val start = System.currentTimeMillis()
        while ((System.currentTimeMillis() - start) < timeoutToWaitConditionMs && valueOrNull != expected) Thread.sleep(100)

        if (valueOrNull == prev) throw TimeoutException("Timeout $timeoutToWaitConditionMs ms while waiting value '$expected'")
        assertEquals(expected, valueOrNull)
    }

    companion object {
        internal const val top = "top"

        internal fun server(lifetime: Lifetime, port: Int? = null): Protocol {
            return Protocol("Server", Serializers(), Identities(IdKind.Server), TestScheduler,
                SocketWire.Server(lifetime, TestScheduler, port, "TestServer"), lifetime
            )
        }


        internal fun client(lifetime: Lifetime, serverProtocol: Protocol): Protocol {
            return Protocol("Client", Serializers(), Identities(IdKind.Client), TestScheduler,
                SocketWire.Client(lifetime,
                    TestScheduler, (serverProtocol.wire as SocketWire.Server).port, "TestClient"), lifetime
            )
        }

        internal fun client(lifetime: Lifetime, port: Int): Protocol {
            return Protocol("Client", Serializers(), Identities(IdKind.Client), TestScheduler,
                SocketWire.Client(lifetime, TestScheduler, port, "TestClient"), lifetime
            )
        }
    }

    lateinit var socketLifetime: Lifetime

    @BeforeEach
    fun setUp() {
        socketLifetime = lifetime.createNested().lifetime
//        ConsoleLoggerFactory.minLevelToLog = LogLevel.Trace
    }

    @Test
    fun TestBasicRun() {
        val serverProtocol = server(socketLifetime)
        val clientProtocol = client(socketLifetime, serverProtocol)

        val sp = RdOptionalProperty<Int>().static(1).apply { bind(lifetime, serverProtocol, top) }
        val cp = RdOptionalProperty<Int>().static(1).apply { bind(lifetime, clientProtocol, top) }

        cp.set(1)
        sp.waitAndAssert(1)

        sp.set(2)
        cp.waitAndAssert(2, 1)
    }

    @Test
    fun TestOrdering() {
        val serverProtocol = server(socketLifetime)
        val clientProtocol = client(socketLifetime, serverProtocol)

        val sp = RdOptionalProperty<Int>().static(1).apply { bind(lifetime, serverProtocol, top) }
        val cp = RdOptionalProperty<Int>().static(1).apply { bind(lifetime, clientProtocol, top) }

        val log = ConcurrentLinkedQueue<Int>()
        sp.advise(lifetime) { log.add(it) }
        cp.set(1)
        cp.set(2)
        cp.set(3)
        cp.set(4)
        cp.set(5)

        while (log.size < 5) Thread.sleep(100)
        assertEquals(listOf(1, 2, 3, 4, 5), log.toList())
    }


    @Disabled
    @Test
    fun TestDisconnect() {
        val serverProtocol = server(socketLifetime)
        val clientProtocol = client(socketLifetime, serverProtocol)

        val sp = RdSignal<Int>().static(1).apply { bind(lifetime, serverProtocol, top) }
        val cp = RdSignal<Int>().static(1).apply { bind(lifetime, clientProtocol, top) }

        val log = mutableListOf<Int>()
        sp.advise(socketLifetime) { log.add(it) }

        cp.fire(1)
        cp.fire(2)

        spinUntil { log.size == 2 }
        assertEquals(listOf(1, 2), log)

        tryCloseConnection(clientProtocol)
        cp.fire(3)
        cp.fire(4)

        spinUntil { log.size == 4 }
        assertEquals(listOf(1, 2, 3, 4), log)


        cp.fire(5)
        tryCloseConnection(serverProtocol)
        cp.fire(6)
        spinUntil { log.size == 6 }
        assertEquals(listOf(1, 2, 3, 4, 5, 6), log)

    }

    private fun tryCloseConnection(protocol: Protocol) {

        val wire = protocol.wire as SocketWire.Base
        if (spinUntil(50) { wire.connected.value }) {
            //if wire is not connected should not ask socket.close because it could lead to IOException
            // 	at java.net.Socket.getOutputStream(Socket.java:951)
            //	at com.jetbrains.rd.framework.SocketWire$Base$1.invoke(SocketWire.kt:102)
            wire.socketProvider.valueOrNull?.close()
        }
    }


    @Test
    fun TestDdos() {
        val serverProtocol = server(socketLifetime)
        val clientProtocol = client(socketLifetime, serverProtocol)

        val sp = RdSignal<Int>().static(1).apply { bind(lifetime, serverProtocol, top) }
        val cp = RdSignal<Int>().static(1).apply { bind(lifetime, clientProtocol, top) }

        var count = 0
        sp.advise(socketLifetime) {
            assertEquals(count + 1, it)
            ++count
        }
        val C = 50
        for (i in 1..C) {
            cp.fire(i)
            if (i == C / 2) {
                tryCloseConnection(serverProtocol)
            }
        }

        spinUntil { count == C }
    }


    @Test
    fun TestBigBuffer() {
        val serverProtocol = server(socketLifetime)
        val clientProtocol = client(socketLifetime, serverProtocol)

        val sp = RdOptionalProperty<String>().static(1).apply { bind(lifetime, serverProtocol, top) }
        val cp = RdOptionalProperty<String>().static(1).apply { bind(lifetime, clientProtocol, top) }

        cp.set("1")
        sp.waitAndAssert("1")

        sp.set("".padStart(100000, '3'))
        cp.waitAndAssert("".padStart(100000, '3'), "1")
    }


    @Test
    fun TestRunWithSlowpokeServer() {

        val port = NetUtils.findFreePort(0)
        val clientProtocol = client(socketLifetime, port)


        val cp = RdOptionalProperty<Int>().static(1).apply { bind(lifetime, clientProtocol, top) }

        cp.set(1)

        Thread.sleep(2000)

        val serverProtocol = server(socketLifetime, port)
        val sp = RdOptionalProperty<Int>().static(1).apply { bind(lifetime, serverProtocol, top) }

        val prev = sp.valueOrNull
        cp.set(4)
        sp.waitAndAssert(4, prev)
    }

    @Test
    fun TestServerWithoutClient() {
        server(socketLifetime)
    }

    @Test
    fun TestServerWithoutClientWithDelay() {
        server(socketLifetime)
        Thread.sleep(100)
    }

    @Test
    fun TestServerWithoutClientWithDelayAndMessages() {
        val protocol = server(socketLifetime)
        Thread.sleep(100)
        val sp = RdOptionalProperty<Int>().static(1).apply { bind(lifetime, protocol, top) }

        sp.set(1)
        sp.set(2)
        Thread.sleep(50)
    }

    @Test
    fun TestClientWithoutServer() {
        client(socketLifetime, NetUtils.findFreePort(0))
    }

    @Test
    fun TestClientWithoutServerWithDelay() {
        client(socketLifetime, NetUtils.findFreePort(0))
        Thread.sleep(100)
    }

    @Test
    fun TestClientWithoutServerWithDelayAndMessages() {
        val clientProtocol = client(socketLifetime, NetUtils.findFreePort(0))

        val cp = RdOptionalProperty<Int>().static(1).apply { bind(lifetime, clientProtocol, top) }

        cp.set(1)
        cp.set(2)
        Thread.sleep(50)
    }

//    @Test
//    fun testReentrantWrites() {
//        val serverProtocol = server(socketLifetime)
//        val clientProtocol = client(socketLifetime, serverProtocol)
//
//        serverProtocol.serializers.register(InterningNestedTestStringModel)
//        clientProtocol.serializers.register(InterningNestedTestStringModel)
//
//        val sp = PropertyHolderWithInternRoot(RdOptionalProperty<InterningNestedTestStringModel>().static(1), serverProtocol.serializationContext)
//        val cp = PropertyHolderWithInternRoot(RdOptionalProperty<InterningNestedTestStringModel>().static(1), clientProtocol.serializationContext)
//
//        sp.mySerializationContext = sp.mySerializationContext.withInternRootsHere(sp, "Test")
//        cp.mySerializationContext = cp.mySerializationContext.withInternRootsHere(cp, "Test")
//
//        sp.bind(lifetime, serverProtocol, "top")
//        cp.bind(lifetime, clientProtocol, "top")
//
//        val modelA = InterningNestedTestStringModel("A", InterningNestedTestStringModel("B", InterningNestedTestStringModel("C", null)))
//        cp.property.set(modelA)
//        sp.property.waitAndAssert(modelA)
//
//        val modelB = InterningNestedTestStringModel("D", InterningNestedTestStringModel("E", InterningNestedTestStringModel("F", null)))
//        sp.property.set(modelB)
//        cp.property.waitAndAssert(modelB, modelA)
//    }


    @Test
    fun testRemoteSocket() {
        val serverSocket = SocketWire.Server(lifetime, TestScheduler, 0, allowRemoteConnections = true)
        val clientSocket = SocketWire.Client(lifetime, TestScheduler, serverSocket.port, hostAddress = InetAddress.getLocalHost())

        assertTrue(spinUntil(60000L) { clientSocket.connected.value })
    }



    @Test
    fun testSocketFactory() {

        val sLifetime = LifetimeDefinition()
        val factory = SocketWire.ServerFactory(sLifetime, TestScheduler, 0)

        val lf1 = LifetimeDefinition()
        SocketWire.Client(lf1, TestScheduler, factory.localPort)

        spinUntil { factory.size == 1 }

        val lf2 = LifetimeDefinition()
        SocketWire.Client(lf2, TestScheduler, factory.localPort)

        spinUntil { factory.size == 2 }

        lf1.terminate()
        spinUntil { factory.size == 1 }

        sLifetime.terminate()
        spinUntil { factory.size == 0 }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testPacketLoss(isClientToServer: Boolean) {
        Lifetime.using { lifetime ->
            val serverProtocol = server(lifetime)
            val serverWire = serverProtocol.wire

            val proxy = SocketProxy("TestProxy", lifetime, serverProtocol)

            proxy.start()

            val clientProtocol = client(lifetime, proxy.port)
            val clientWire = clientProtocol.wire

            Thread.sleep(100)

            if (isClientToServer)
                proxy.stopClientToServerMessaging()
            else
                proxy.stopServerToClientMessaging()

            val detectionTimeoutMs = clientProtocol.wire.heartbeatIntervalMs * (SocketWire.maximumHeartbeatDelay + 3)

            Thread.sleep(detectionTimeoutMs)

            assertTrue(serverWire.connected.value)
            assertTrue(clientWire.connected.value)

            assertFalse(serverWire.heartbeatAlive.value)
            assertFalse(clientWire.heartbeatAlive.value)

            if (isClientToServer)
                proxy.startClientToServerMessaging()
            else
                proxy.startServerToClientMessaging()

            Thread.sleep(detectionTimeoutMs)

            assertTrue(serverWire.connected.value)
            assertTrue(clientWire.connected.value)

            assertTrue(serverWire.heartbeatAlive.value)
            assertTrue(clientWire.heartbeatAlive.value)
        }
    }


//    @BeforeClass
//    fun beforeClass() {
//         setupLogHandler {
//        if (it.getLevel() == Level.ERROR) {
//            System.err.println(it.message)
//            it.throwableInformation?.throwable?.printStackTrace()
//        }
//         }
//    }
//
//    private fun setupLogHandler(name: String = "default", action: (LoggingEvent) -> Unit) {
//         val rootLogger = org.apache.log4j.Logger.getRootLogger()
//         rootLogger.removeAppender("default")
//         rootLogger.addAppender(object : AppenderSkeleton() {
//        init {
//            setName(name)
//        }
//
//        override fun append(event: LoggingEvent) {
//            action(event)
//        }
//
//        override fun close() {}
//
//        override fun requiresLayout(): Boolean {
//            return false
//        }
//         })
//    }
}
