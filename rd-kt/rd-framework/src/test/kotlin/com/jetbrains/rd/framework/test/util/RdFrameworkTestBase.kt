package com.jetbrains.rd.framework.test.util

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.framework.base.bindTopLevel
import com.jetbrains.rd.util.Closeable
import com.jetbrains.rd.util.ILoggerFactory
import com.jetbrains.rd.util.Statics
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.log.ErrorAccumulatorLoggerFactory
import com.jetbrains.rd.util.reactive.ExecutionOrder
import com.jetbrains.rd.util.reactive.IScheduler
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

object TestScheduler : IScheduler {
    override fun flush() {}
    override fun queue(action: () -> Unit) = action()
    override val isActive: Boolean get() = true
    override val executionOrder: ExecutionOrder
        get() = ExecutionOrder.Unknown
}

open class RdFrameworkTestBase {


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

    protected open val clientWireScheduler: IScheduler? get() = null
    protected open val serverWireScheduler: IScheduler? get() = null

    @BeforeTest
    fun setUp() {
        disposeLoggerFactory = Statics<ILoggerFactory>().push(ErrorAccumulatorLoggerFactory)
        clientLifetimeDef = Lifetime.Eternal.createNested()
        serverLifetimeDef = Lifetime.Eternal.createNested()


        val clientTestWire = TestWire(clientWireScheduler ?: clientScheduler)
        val serverTestWire = TestWire(serverWireScheduler ?: serverScheduler)

        val (w1, w2) = clientTestWire to serverTestWire
        w1.counterpart = w2
        w2.counterpart = w1

        clientProtocol = Protocol("Client", createSerializers(false),
                SequentialIdentities(IdKind.Client),
            clientScheduler, clientTestWire, clientLifetime)

        serverProtocol = Protocol("Server", createSerializers(true),
                SequentialIdentities(IdKind.Server),
            serverScheduler, serverTestWire, serverLifetime)
    }

    open fun createSerializers(isServer: Boolean): ISerializers {
        return Serializers()
    }

    @AfterTest
    open fun tearDown() {
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
        AllowBindingCookie.allowBind {
            x.bindTopLevel(lf, this, name)
        }
        return x
    }

    internal fun <T : RdBindableBase> IProtocol.bindStatic(x: T, id: Int): T {
        val lf = when (this) {
            clientProtocol -> clientLifetime
            serverProtocol -> serverLifetime
            else -> throw IllegalArgumentException("Not valid protocol, must be client or server")
        }
        AllowBindingCookie.allowBind {
            x.static(id).bindTopLevel(lf, this, "top")
        }

        return x
    }


    protected fun setWireAutoFlush(flag: Boolean) {
        clientWire.autoFlush = flag
        serverWire.autoFlush = flag
    }
}