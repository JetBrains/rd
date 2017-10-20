package com.jetbrains.rider.framework.test.cases.wire

import com.jetbrains.rider.framework.*
import com.jetbrains.rider.framework.base.static
import com.jetbrains.rider.framework.impl.RdProperty
import com.jetbrains.rider.framework.test.cases.TestScheduler
import com.jetbrains.rider.util.Maybe
import com.jetbrains.rider.util.NetUtils
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.reactive.set
import org.apache.log4j.AppenderSkeleton
import org.apache.log4j.Level
import org.apache.log4j.spi.LoggingEvent
import org.testng.Assert
import org.testng.annotations.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeoutException
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class SocketWireTest {

    private fun <T> RdProperty<T>.waitAndAssert(expected: T, prev: Maybe<T> = Maybe.None) {
        val start = System.currentTimeMillis()
        val timeout = 5000
        while ((System.currentTimeMillis() - start) < timeout && maybe != Maybe.Just(expected)) Thread.sleep(100)

        if (maybe == prev) throw TimeoutException("Timeout $timeout ms while waiting value '$expected'")
        assertEquals(expected, value)
    }

    private fun server(lifetime: Lifetime, port: Int? = null): Protocol {
        return Protocol(Serializers(), Identities(IdKind.DynamicServer), TestScheduler) {
            protocol ->
            SocketWire.Server(lifetime, protocol, port, "TestServer")
        }
    }


    private fun client(lifetime: Lifetime, serverProtocol: Protocol): Protocol {
        return Protocol(Serializers(), Identities(IdKind.DynamicClient), TestScheduler) {
            protocol ->
            SocketWire.Client(lifetime, protocol, (serverProtocol.wire as SocketWire.Server).port, "TestClient")
        }
    }

    private fun client(lifetime: Lifetime, port: Int): Protocol {
        return Protocol(Serializers(), Identities(IdKind.DynamicClient), TestScheduler) {
            protocol ->
            SocketWire.Client(lifetime, protocol, port, "TestClient")
        }
    }

    @AfterClass
    fun restoreLoggerAppenders() {
        val rootLogger = org.apache.log4j.Logger.getRootLogger()
        rootLogger.removeAllAppenders()
    }

    @Test
    fun TestBasicRun() {
        Lifetime.using { lifetime ->
            val serverProtocol = server(lifetime)
            val clientProtocol = client(lifetime, serverProtocol)

            val sp = RdProperty<Int>().static(1).apply { bind(lifetime, serverProtocol, "top") }
            val cp = RdProperty<Int>().static(1).apply { bind(lifetime, clientProtocol, "top") }

            cp.set(1)
            sp.waitAndAssert(1)

            sp.set(2)
            cp.waitAndAssert(2, Maybe.Just(1))
        }
    }

    @Test
    fun TestOrdering() {
        Lifetime.using { lifetime ->
            val serverProtocol = server(lifetime)
            val clientProtocol = client(lifetime, serverProtocol)

            val sp = RdProperty<Int>().static(1).apply { bind(lifetime, serverProtocol, "top") }
            val cp = RdProperty<Int>().static(1).apply { bind(lifetime, clientProtocol, "top") }

            val log = ConcurrentLinkedQueue<Int>()
            sp.advise(lifetime) {log.add(it)}
            cp.set(1)
            cp.set(2)
            cp.set(3)
            cp.set(4)
            cp.set(5)

            while (log.size < 5) Thread.sleep(100)
            assertEquals(listOf(1, 2, 3, 4, 5), log.toList())
        }
    }


    @Test
    fun TestBigBuffer() {
        Lifetime.using { lifetime ->
            val serverProtocol = server(lifetime)
            val clientProtocol = client(lifetime, serverProtocol)

            val sp = RdProperty<String>().static(1).apply { bind(lifetime, serverProtocol, "top") }
            val cp = RdProperty<String>().static(1).apply { bind(lifetime, clientProtocol, "top") }

            cp.set("1")
            sp.waitAndAssert("1")

            sp.set("".padStart(100000, '3'))
            cp.waitAndAssert("".padStart(100000, '3'), Maybe.Just("1"))
        }
    }


    @Test
    fun TestRunWithSlowpokeServer() {
        Lifetime.using { lifetime ->

            val port = NetUtils.findFreePort(0)
            val clientProtocol = client(lifetime, port)


            val cp = RdProperty<Int>().static(1).apply { bind(lifetime, clientProtocol, "top") }

            cp.set(1)

            Thread.sleep(2000)

            val serverProtocol = server(lifetime, port)
            val sp = RdProperty<Int>().static(1).apply { bind(lifetime, serverProtocol, "top") }

            val prev = sp.maybe
            cp.set(4)
            sp.waitAndAssert(4, prev)
        }
    }

    @Test
    fun TestServerWithoutClient() {
        Lifetime.using { lifetime ->
            server(lifetime)
        }
    }

    @Test
    fun TestServerWithoutClientWithDelay() {
        Lifetime.using { lifetime ->
            server(lifetime)
            Thread.sleep(100)
        }
    }

    @Test
    fun TestServerWithoutClientWithDelayAndMessages() {
        Lifetime.using { lifetime ->
            val protocol = server(lifetime)
            Thread.sleep(100)
            val sp = RdProperty<Int>().static(1).apply { bind(lifetime, protocol, "top") }

            sp.set(1)
            sp.set(2)
            Thread.sleep(50)
        }
    }

    @Test
    fun TestClientWithoutServer() {
        Lifetime.using { lifetime ->
            client(lifetime, NetUtils.findFreePort(0))
        }
    }

    @Test
    fun TestClientWithoutServerWithDelay() {
        Lifetime.using { lifetime ->
            client(lifetime, NetUtils.findFreePort(0))
            Thread.sleep(100)
        }
    }

    @Test
    fun TestClientWithoutServerWithDelayAndMessages() {
        Lifetime.using { lifetime ->
            val clientProtocol = client(lifetime, NetUtils.findFreePort(0))

            val cp = RdProperty<Int>().static(1).apply { bind(lifetime, clientProtocol, "top") }

            cp.set(1)
            cp.set(2)
            Thread.sleep(50)
        }
    }


    private fun setupLogHandler(name: String = "default", action: (LoggingEvent) -> Unit) {
        val rootLogger = org.apache.log4j.Logger.getRootLogger()
        rootLogger.removeAppender("default")
        rootLogger.addAppender(object: AppenderSkeleton() {
            init {
                setName(name)
            }

            override fun append(event: LoggingEvent) {
                action(event)
            }

            override fun close() {}

            override fun requiresLayout(): Boolean { return false }
        })
    }
//
//    @BeforeClass
//    fun setUp() {
//        setupLogHandler("globalErrors") {
//            if (it.getLevel() == Level.ERROR ||
//                it.getLevel() == Level.WARN ||
//                it.getLevel() == Level.FATAL) {
//                Assert.fail("Found logged error", it.throwableInformation?.throwable)
//            }
//        }
//    }
//
//    @Test
//    fun ServerShouldNotSendAnythingAfterDisconnect() {
//        var brokenPipeInfoMessageCount = 0
//        setupLogHandler {
//            println(it.message)
//            if (it.message == "Broken pipe" ||
//                it.message == "Software caused connection abort: socket write error") {
//                brokenPipeInfoMessageCount++
//            }
//        }
//        Lifetime.using { serverLifetime ->
//            val serverProtocol = server(serverLifetime)
//            val sp = RdProperty<Int>().static(1).apply { bind(serverLifetime, serverProtocol, "top") }
//            Lifetime.using { clientLifetime ->
//                val clientProtocol = client(clientLifetime, serverProtocol)
//                val cp = RdProperty<Int>().static(1).apply { bind(clientLifetime, clientProtocol, "top") }
//                sp.set(2)
//                cp.waitAndAssert(2)
//            }
//            sp.set(10)
//            sp.set(11)
//        }
//        // Broken pipe should fire once or none
//        assertTrue(brokenPipeInfoMessageCount == 0 ||
//                brokenPipeInfoMessageCount == 1)
//    }
//
//    @Test
//    fun ClientShouldNotSendAnythingAfterDisconnect() {
//        var brokenPipeInfoMessageCount = 0
//        setupLogHandler {
//            if (it.message == "Broken pipe" ||
//             it.message == "Software caused connection abort: socket write error") {
//                brokenPipeInfoMessageCount++
//            }
//        }
//        Lifetime.using { clientLifetime ->
//            val serverLifetimeDef = Lifetime.create(clientLifetime)
//            val serverLifetime = serverLifetimeDef.lifetime
//            val serverProtocol = server(serverLifetime)
//            val sp = RdProperty<Int>().static(1).apply { bind(serverLifetime, serverProtocol, "top") }
//
//            val clientProtocol = client(clientLifetime, serverProtocol)
//            val cp = RdProperty<Int>().static(1).apply { bind(clientLifetime, clientProtocol, "top") }
//
//            sp.set(2)
//            cp.waitAndAssert(2)
//
//            serverLifetimeDef.terminate()
//
//            cp.set(10)
//            cp.set(11)
//
//            // Broken pipe should fire once or none
//            assertTrue(brokenPipeInfoMessageCount == 0 ||
//                    brokenPipeInfoMessageCount == 1)
//        }
//    }
}
