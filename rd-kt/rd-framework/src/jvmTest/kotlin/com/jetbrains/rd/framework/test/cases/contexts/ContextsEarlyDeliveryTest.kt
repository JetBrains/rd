package com.jetbrains.rd.framework.test.cases.contexts

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.IRdBindable
import com.jetbrains.rd.framework.base.RdBindableBase
import com.jetbrains.rd.framework.base.static
import com.jetbrains.rd.framework.impl.RdSignal
import com.jetbrains.rd.framework.test.util.RdFrameworkTestBase
import com.jetbrains.rd.framework.test.util.TestScheduler
import com.jetbrains.rd.framework.test.util.TestWire
import com.jetbrains.rd.util.ILoggerFactory
import com.jetbrains.rd.util.Statics
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.log.ErrorAccumulatorLoggerFactory
import com.jetbrains.rd.util.reactive.IScheduler
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.Closeable
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

class ContextsEarlyDeliveryTest {
    object TestKeyHeavy : RdContext<String>("test-key", true, FrameworkMarshallers.String)
    object TestKeyLight : RdContext<String>("test-key", false, FrameworkMarshallers.String)

    private val serializers = Serializers()


    protected lateinit var clientProtocol: IProtocol
        private set


    protected lateinit var serverProtocol: IProtocol
        private set

    protected lateinit var clientWire: TestWire
    protected lateinit var serverWire: TestWire


    protected lateinit var clientLifetimeDef: LifetimeDefinition
        private set

    protected lateinit var serverLifetimeDef: LifetimeDefinition
        private set

    protected val clientLifetime : Lifetime get() = clientLifetimeDef.lifetime
    protected val serverLifetime : Lifetime get() = serverLifetimeDef.lifetime

    private var disposeLoggerFactory : Closeable? = null

    protected val clientScheduler: IScheduler get() = TestScheduler
    protected val serverScheduler: IScheduler get() = TestScheduler

    @BeforeTest
    fun setUp() {
        disposeLoggerFactory = Statics<ILoggerFactory>().push(ErrorAccumulatorLoggerFactory)
        clientLifetimeDef = Lifetime.Eternal.createNested()
        serverLifetimeDef = Lifetime.Eternal.createNested()


        clientWire = TestWire(clientScheduler)
        serverWire = TestWire(serverScheduler)

        val (w1, w2) = clientWire to serverWire
        w1.counterpart = w2
        w2.counterpart = w1
    }

    @AfterTest
    fun tearDown() {
        disposeLoggerFactory?.close()
        disposeLoggerFactory = null

        clientLifetimeDef.terminate()
        serverLifetimeDef.terminate()
        ErrorAccumulatorLoggerFactory.throwAndClear()
    }

    internal fun <T : IRdBindable> IProtocol.bindStatic(x: T, name: String): T {
        val lf = when (this) {
            clientProtocol -> clientLifetime
            serverProtocol -> serverLifetime
            else -> throw IllegalArgumentException("Not valid protocol, must be client or server")
        }
        x.bind(lf, this, name)
        return x
    }

    internal fun <T : RdBindableBase> IProtocol.bindStatic(x: T, id: Int, isServer: Boolean): T {
        val lf = when (isServer) {
            false -> clientLifetime
            true -> serverLifetime
        }
        x.static(id).bind(lf, this, "top")
        return x
    }


    protected fun setWireAutoFlush(flag: Boolean) {
        clientWire.autoFlush = flag
        serverWire.autoFlush = flag
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testEarlyDelivery(heavy: Boolean) {
        println("Heavy: $heavy")
        val key = if(heavy) TestKeyHeavy else TestKeyLight

        serverProtocol = Protocol("Server", serializers,
            Identities(IdKind.Server),
            serverScheduler, serverWire, serverLifetime, key)

        val serverSignal = RdSignal<String>()

        serverProtocol.bindStatic(serverSignal, 1, true)

        key.value = "1"

        serverSignal.fire("")

        clientProtocol = Protocol("Client", serializers,
            Identities(IdKind.Client),
            clientScheduler, clientWire, clientLifetime, key)
        val clientSignal = RdSignal<String>()
        clientProtocol.bindStatic(clientSignal, 1, false)

        Lifetime.using { lt ->
            var fired = false
            clientSignal.advise(lt) {
                assert(key.value == "1")
                fired = true
            }

            serverSignal.fire("")
            assert(fired)
        }

        assert(key.value == "1")

        Lifetime.using { lt ->
            var fired = false
            serverSignal.advise(lt) {
                assert(key.value == "1")
                fired = true
            }

            clientSignal.fire("")
            assert(fired)
        }

        assert(key.value == "1")
    }
}