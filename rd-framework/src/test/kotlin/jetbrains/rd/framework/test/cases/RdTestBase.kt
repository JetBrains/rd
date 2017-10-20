package com.jetbrains.rider.framework.test.cases

import com.jetbrains.rider.framework.*
import com.jetbrains.rider.framework.base.IRdBindable
import com.jetbrains.rider.framework.base.IRdReactive
import com.jetbrains.rider.framework.base.withId
import com.jetbrains.rider.framework.test.util.TestWire
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.lifetime.LifetimeDefinition
import com.jetbrains.rider.util.reactive.IScheduler
import com.jetbrains.rider.util.threading.TestSingleThreadScheduler
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod

object TestScheduler : IScheduler {
    override fun queue(action: () -> Unit) = action()
    override val isActive: Boolean get() = true
}

open class RdTestBase(val asyncMode: Boolean = false) {
    private val serializers = Serializers()


    protected lateinit var clientProtocol: IProtocol
        private set


    protected lateinit var serverProtocol: IProtocol
        private set


    protected lateinit var clientLifetimeDef: LifetimeDefinition
        private set

    protected lateinit var serverLifetimeDef: LifetimeDefinition
        private set

    val clientBgScheduler: TestSingleThreadScheduler = TestSingleThreadScheduler("ClientBg")
    val clientUiScheduler: TestSingleThreadScheduler = TestSingleThreadScheduler("ClientUi")
    val serverBgScheduler: TestSingleThreadScheduler = TestSingleThreadScheduler("ServerBg")
    val serverUiScheduler: TestSingleThreadScheduler = TestSingleThreadScheduler("ServerUi")




    protected val clientWire : TestWire get() = clientProtocol.wire as TestWire
    protected val serverWire : TestWire get() = serverProtocol.wire as TestWire

    protected val clientLifetime : Lifetime get() = clientLifetimeDef.lifetime
    protected val serverLifetime : Lifetime get() = serverLifetimeDef.lifetime


    @BeforeMethod
    fun setUp() {
        clientLifetimeDef = Lifetime.create(Lifetime.Eternal)
        serverLifetimeDef = Lifetime.create(Lifetime.Eternal)


        clientProtocol = Protocol(serializers,
                Identities(IdKind.DynamicClient),
                if (asyncMode) clientUiScheduler else TestScheduler, ::TestWire)

        serverProtocol = Protocol(serializers,
                Identities(IdKind.DynamicServer),
                if (asyncMode) serverUiScheduler else TestScheduler, ::TestWire)

        val (w1, w2) = (clientProtocol.wire as TestWire) to (serverProtocol.wire as TestWire)
        w1.counterpart = w2
        w2.counterpart = w1
    }

    @AfterMethod
    fun tearDown() {
        clientBgScheduler.assertNoExceptions()
        serverBgScheduler.assertNoExceptions()
        clientUiScheduler.assertNoExceptions()
        serverUiScheduler.assertNoExceptions()
        clientLifetimeDef.terminate()
        serverLifetimeDef.terminate()
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

    internal fun <T : IRdReactive> IProtocol.bindStatic(x: T, id: Int): T {
        val lf = when (this) {
            clientProtocol -> clientLifetime
            serverProtocol -> serverLifetime
            else -> throw IllegalArgumentException("Not valid protocol, must be client or server")
        }
        x.withId(RdId(IdKind.StaticEntity, id)).bind(lf, this, "top")
        return x
    }


    protected fun setWireAutoFlush(flag: Boolean) {
        clientWire.autoFlush = flag
        serverWire.autoFlush = flag
    }
}