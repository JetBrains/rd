package com.jetbrains.rd.framework.base

import com.jetbrains.rd.framework.AbstractBuffer
import com.jetbrains.rd.framework.IContextAwareWire
import com.jetbrains.rd.framework.MessageBroker
import com.jetbrains.rd.framework.RdId
import com.jetbrains.rd.framework.impl.ProtocolContexts
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.reactive.Property
import com.jetbrains.rd.util.string.printToString


abstract class WireBase(val scheduler: IScheduler) : IContextAwareWire {
    override val connected = Property(false)
    protected val messageBroker = MessageBroker(scheduler)

    override fun advise(lifetime: Lifetime, entity: IRdWireable) = messageBroker.adviseOn(lifetime, entity)

    fun dumpToString() = messageBroker.printToString()

    override var contexts: ProtocolContexts? = null
        set(value) {
            require(field == null) { "Can't replace ProtocolContexts in IContextAwareWire"}
            field = value
        }

    abstract override fun send(id: RdId, writer: (AbstractBuffer) -> Unit)
}