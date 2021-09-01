package com.jetbrains.rd.framework.test.util

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.IRdBindable
import com.jetbrains.rd.framework.base.RdBindableBase
import com.jetbrains.rd.framework.base.static
import com.jetbrains.rd.util.Closeable
import com.jetbrains.rd.util.ILoggerFactory
import com.jetbrains.rd.util.Statics
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.log.ErrorAccumulatorLoggerFactory
import com.jetbrains.rd.util.reactive.IScheduler
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

object TestScheduler : IScheduler {
    override fun flush() {}
    override fun queue(action: () -> Unit) = action()
    override val isActive: Boolean get() = true
}

open class RdFrameworkTestBase {
    private val serializers = Serializers()


    protected lateinit var clientProtocol: IProtocol
        private set


    protected lateinit var serverProtocol: IProtocol
        private set


    protected lateinit var clientLifetimeDef: LifetimeDefinition
        private set

    protected lateinit var serverLifetimeDef: LifetimeDefinition
        private set



    protected val clientWire : TestWire get() = clientProtocol.wire as TestWire
    protected val serverWire : TestWire get() = serverProtocol.wire as TestWire

    protected val clientLifetime : Lifetime get() = clientLifetimeDef.lifetime
    protected val serverLifetime : Lifetime get() = serverLifetimeDef.lifetime

    private var disposeLoggerFactory : Closeable? = null

    protected open val clientScheduler: IScheduler get() = TestScheduler
    protected open val serverScheduler: IScheduler get() = TestScheduler

    @BeforeTest
    fun setUp() {
        disposeLoggerFactory = Statics<ILoggerFactory>().push(ErrorAccumulatorLoggerFactory)
        clientLifetimeDef = Lifetime.Eternal.createNested()
        serverLifetimeDef = Lifetime.Eternal.createNested()


        val clientTestWire = TestWire(clientScheduler)
        val serverTestWire = TestWire(serverScheduler)

        val (w1, w2) = clientTestWire to serverTestWire
        w1.counterpart = w2
        w2.counterpart = w1

        clientProtocol = Protocol("Client", serializers,
                Identities(IdKind.Client),
            clientScheduler, clientTestWire, clientLifetime)

        serverProtocol = Protocol("Server", serializers,
                Identities(IdKind.Server),
            serverScheduler, serverTestWire, serverLifetime)
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

    internal fun <T : RdBindableBase> IProtocol.bindStatic(x: T, id: Int): T {
        val lf = when (this) {
            clientProtocol -> clientLifetime
            serverProtocol -> serverLifetime
            else -> throw IllegalArgumentException("Not valid protocol, must be client or server")
        }
        x.static(id).bind(lf, this, "top")
        return x
    }


    protected fun setWireAutoFlush(flag: Boolean) {
        clientWire.autoFlush = flag
        serverWire.autoFlush = flag
    }
}