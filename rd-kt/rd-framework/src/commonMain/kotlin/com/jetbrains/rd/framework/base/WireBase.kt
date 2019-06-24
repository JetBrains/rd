package com.jetbrains.rd.framework.base

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.reactive.Property
import com.jetbrains.rd.util.string.printToString


abstract class WireBase(val scheduler: IScheduler) : IWire {
    override val connected = Property(false)
    protected val messageBroker = MessageBroker(scheduler)

    override fun advise(lifetime: Lifetime, entity: IRdWireable) = messageBroker.adviseOn(lifetime, entity)

    fun dumpToString() = messageBroker.printToString()
}