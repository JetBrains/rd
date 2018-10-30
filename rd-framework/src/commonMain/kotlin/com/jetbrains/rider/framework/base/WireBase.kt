package com.jetbrains.rider.framework.base

import com.jetbrains.rider.framework.*
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.reactive.IScheduler
import com.jetbrains.rider.util.reactive.Property
import com.jetbrains.rider.util.string.printToString


abstract class WireBase(val scheduler: IScheduler) : IWire {
    override val connected = Property(false)
    protected val messageBroker = MessageBroker(scheduler)

    override fun advise(lifetime: Lifetime, entity: IRdReactive) = messageBroker.adviseOn(lifetime, entity)

    fun dumpToString() = messageBroker.printToString()
}