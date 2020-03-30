package com.jetbrains.rd.framework.base

import com.jetbrains.rd.framework.AbstractBuffer
import com.jetbrains.rd.framework.IWire
import com.jetbrains.rd.framework.MessageBroker
import com.jetbrains.rd.framework.RdId
import com.jetbrains.rd.framework.impl.ProtocolContexts
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.reactive.Property
import com.jetbrains.rd.util.string.printToString
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds


abstract class WireBase(val scheduler: IScheduler) : IWire {
    private lateinit var contextsInternal: ProtocolContexts
    final override val connected = Property(false)
    override val heartbeatAlive = Property(false)
    protected val messageBroker = MessageBroker(scheduler)

    override fun advise(lifetime: Lifetime, entity: IRdWireable) = messageBroker.adviseOn(lifetime, entity)

    fun dumpToString() = messageBroker.printToString()

    override val contexts: ProtocolContexts
        get() = contextsInternal

    /**
     * Ping's interval and not actually detection's timeout.
     * Its value must be the same on both sides of connection.
     */
    @UseExperimental(ExperimentalTime::class)
    override var heartbeatInterval = 500.milliseconds

    abstract override fun send(id: RdId, writer: (AbstractBuffer) -> Unit)

    override fun setupContexts(newContexts: ProtocolContexts) {
        require(!this::contextsInternal.isInitialized) { "Can't replace ProtocolContexts in IWire"}
        contextsInternal = newContexts
    }
}
