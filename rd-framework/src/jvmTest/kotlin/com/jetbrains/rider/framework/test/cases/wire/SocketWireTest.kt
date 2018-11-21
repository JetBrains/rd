package com.jetbrains.rider.framework.test.cases.wire

import com.jetbrains.rider.framework.*
import com.jetbrains.rider.framework.base.RdBindableBase
import com.jetbrains.rider.framework.base.static
import com.jetbrains.rider.framework.impl.RdOptionalProperty
import com.jetbrains.rider.framework.impl.RdProperty
import com.jetbrains.rider.framework.test.util.TestScheduler
import com.jetbrains.rider.framework.test.util.NetUtils
import com.jetbrains.rider.plugins.unrealengine.UnrealEngineModel
import com.jetbrains.rider.util.eol
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.lifetime.LifetimeDefinition
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeoutException
import kotlin.test.assertEquals


class SocketWireTest {

    private fun <T : Any> RdOptionalProperty<T>.waitAndAssert(expected: T, prev: T? = null) {
        val start = System.currentTimeMillis()
        val timeout = 5000
        while ((System.currentTimeMillis() - start) < timeout && valueOrNull != expected) Thread.sleep(100)

        if (valueOrNull == prev) throw TimeoutException("Timeout $timeout ms while waiting value '$expected'")
        assertEquals(expected, valueOrNull)
    }

    private fun server(lifetime: Lifetime, port: Int? = null): Protocol {
        return Protocol(Serializers(), Identities(IdKind.Server), TestScheduler,
            SocketWire.Server(lifetime, TestScheduler, port, "TestServer")
        )
    }


    private fun client(lifetime: Lifetime, serverProtocol: Protocol): Protocol {
        return Protocol(Serializers(), Identities(), TestScheduler,
            SocketWire.Client(lifetime,
                    TestScheduler, (serverProtocol.wire as SocketWire.Server).port, "TestClient")
        )
    }

    private fun client(lifetime: Lifetime, port: Int): Protocol {
        return Protocol(Serializers(), Identities(), TestScheduler,
            SocketWire.Client(lifetime, TestScheduler, port, "TestClient")
        )
    }

    private lateinit var lifetimeDef: LifetimeDefinition
    private lateinit var socketLifetimeDef: LifetimeDefinition

    val lifetime : Lifetime get() = lifetimeDef.lifetime
    val socketLifetime : Lifetime get() = socketLifetimeDef.lifetime

    @Before
    fun setUp() {
        lifetimeDef = Lifetime.create(Lifetime.Eternal)
        socketLifetimeDef = Lifetime.create(Lifetime.Eternal)
    }


    @After
    fun tearDown() {
        socketLifetimeDef.terminate()
        lifetimeDef.terminate()
    }


    @Test
    fun TestBasicRun() {
            val serverProtocol = server(socketLifetime)
            val clientProtocol = client(socketLifetime, serverProtocol)

            val sp = RdOptionalProperty<Int>().static(1).apply { bind(lifetime, serverProtocol, "top") }
            val cp = RdOptionalProperty<Int>().static(1).apply { bind(lifetime, clientProtocol, "top") }

            cp.set(1)
            sp.waitAndAssert(1)

            sp.set(2)
            cp.waitAndAssert(2, 1)
    }

    @Test
    fun TestOrdering() {
            val serverProtocol = server(socketLifetime)
            val clientProtocol = client(socketLifetime, serverProtocol)

            val sp = RdOptionalProperty<Int>().static(1).apply { bind(lifetime, serverProtocol, "top") }
            val cp = RdOptionalProperty<Int>().static(1).apply { bind(lifetime, clientProtocol, "top") }

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


    @Test
    fun TestBigBuffer() {
            val serverProtocol = server(socketLifetime)
            val clientProtocol = client(socketLifetime, serverProtocol)

            val sp = RdOptionalProperty<String>().static(1).apply { bind(lifetime, serverProtocol, "top") }
            val cp = RdOptionalProperty<String>().static(1).apply { bind(lifetime, clientProtocol, "top") }

            cp.set("1")
            sp.waitAndAssert("1")

            sp.set("".padStart(100000, '3'))
            cp.waitAndAssert("".padStart(100000, '3'), "1")
    }


    @Test
    fun TestRunWithSlowpokeServer() {

            val port = NetUtils.findFreePort(0)
            val clientProtocol = client(socketLifetime, port)


            val cp = RdOptionalProperty<Int>().static(1).apply { bind(lifetime, clientProtocol, "top") }

            cp.set(1)

            Thread.sleep(2000)

            val serverProtocol = server(socketLifetime, port)
            val sp = RdOptionalProperty<Int>().static(1).apply { bind(lifetime, serverProtocol, "top") }

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
            val sp = RdOptionalProperty<Int>().static(1).apply { bind(lifetime, protocol, "top") }

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

            val cp = RdOptionalProperty<Int>().static(1).apply { bind(lifetime, clientProtocol, "top") }

            cp.set(1)
            cp.set(2)
            Thread.sleep(50)
    }

    @Test
    fun TestRecieveFromCppClient() {
        val clientProtocol = server(socketLifetime, NetUtils.findFreePort(0))
        File("C:\\temp\\port.txt").printWriter().use { out ->
            out.print((clientProtocol.wire as SocketWire.Server).port)
        }
        val property_main = RdProperty(0, FrameworkMarshallers.Int32.nullable()).static(1).apply { bind(lifetime, clientProtocol, "main") }
        val property_rx = RdProperty(0, FrameworkMarshallers.Int32.nullable()).static(2).apply { bind(lifetime, clientProtocol, "rx") }

        property_main.advise(lifetime) {
            property_rx.value = it
        }

        println("advised")

        Thread.sleep(500000)
    }

    @Test
    fun TestUnrealEngineModelServer() {
        val appLifetime = Lifetime.Eternal.createNested()
        val protocol = server(socketLifetime, NetUtils.findFreePort(0))
        val model = UnrealEngineModel.create(appLifetime, protocol)
        File("C:\\temp\\port.txt").printWriter().use { out ->
            out.println((protocol.wire as SocketWire.Server).port)

            val rdid_test_connection = (model.test_connection as RdBindableBase).rdid.toString()
            val rdid_filename_to_open = (model.filename_to_open as RdBindableBase).rdid.toString()
            out.println(rdid_test_connection)
            out.println(rdid_filename_to_open)
        }



        model.test_connection.advise(appLifetime) {
            println("Connection UE $it")
        }
        model.filename_to_open.advise(appLifetime) {
            println("rdid_filename_to_open changed: $it")
        }
        Thread.sleep(500000)
    }

    @Test
    fun TestUnrealEngineModelClient() {
        var port = 0
        File("C:\\temp\\port.txt").bufferedReader().use { input ->
            port = input.readLine().toInt()
        }

        val appLifetime = Lifetime.Eternal.createNested()
        val protocol = client(socketLifetime, port)
        val model = UnrealEngineModel.create(appLifetime, protocol)


        var newValue : Int = 0xdeadbeef.toInt()
        model.test_connection.set(newValue)
        model.filename_to_open.set("beefdeadx0")
        Thread.sleep(500000)
    }
//    @BeforeClass
//    fun beforeClass() {
//        setupLogHandler {
//            if (it.getLevel() == Level.ERROR) {
//                System.err.println(it.message)
//                it.throwableInformation?.throwable?.printStackTrace()
//            }
//        }
//    }
//
//    private fun setupLogHandler(name: String = "default", action: (LoggingEvent) -> Unit) {
//        val rootLogger = org.apache.log4j.Logger.getRootLogger()
//        rootLogger.removeAppender("default")
//        rootLogger.addAppender(object : AppenderSkeleton() {
//            init {
//                setName(name)
//            }
//
//            override fun append(event: LoggingEvent) {
//                action(event)
//            }
//
//            override fun close() {}
//
//            override fun requiresLayout(): Boolean {
//                return false
//            }
//        })
//    }
}
