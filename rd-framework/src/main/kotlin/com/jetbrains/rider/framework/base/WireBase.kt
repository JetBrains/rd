package com.jetbrains.rider.framework.base

import com.jetbrains.rider.framework.*
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.reactive.IScheduler
import com.jetbrains.rider.util.reactive.Property
import com.jetbrains.rider.util.string.printToString


abstract class WireBase(val scheduler: IScheduler) : IWire {
    override val connected = Property(false)
    protected val messageBroker = MessageBroker(scheduler)

    override fun advise(lifetime: Lifetime, id: RdId, handler: (AbstractBuffer) -> Unit) = adviseOn(lifetime, id, scheduler, handler)
    override fun adviseOn(lifetime: Lifetime, id: RdId, scheduler: IScheduler, handler: (AbstractBuffer) -> Unit) = messageBroker.adviseOn(lifetime, scheduler, id, handler)

    fun dumpToString() = messageBroker.printToString()
}