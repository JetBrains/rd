package com.jetbrains.rd.framework.base

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.impl.ProtocolContexts
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.reactive.Property
import com.jetbrains.rd.util.string.printToString

abstract class WireBase(val scheduler: IScheduler) : IWireWithDelayedDelivery {
    private lateinit var contextsInternal: ProtocolContexts
    final override val connected = Property(false)
    override val heartbeatAlive = Property(false)
    protected val messageBroker = MessageBroker(scheduler, true)

    override fun advise(lifetime: Lifetime, entity: IRdWireable) = messageBroker.adviseOn(lifetime, entity)

    fun dumpToString() = messageBroker.printToString()

    override fun startDeliveringMessages() = messageBroker.startDeliveringMessages()

    override val contexts: ProtocolContexts
        get() = contextsInternal

    /**
     * Ping's interval and not actually detection's timeout.
     * Its value must be the same on both sides of connection.
     */
    override var heartbeatIntervalMs = 500L

    abstract override fun send(id: RdId, writer: (AbstractBuffer) -> Unit)

    override fun setupContexts(newContexts: ProtocolContexts) {
        require(!this::contextsInternal.isInitialized) { "Can't replace ProtocolContexts in IWire"}
        contextsInternal = newContexts
    }
}
