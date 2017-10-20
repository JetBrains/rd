package com.jetbrains.rider.framework.base

import com.jetbrains.rider.framework.*
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.reactive.IScheduler
import com.jetbrains.rider.util.reactive.Property
import java.io.InputStream


abstract class WireBase(private val scheduler: IScheduler) : IWire {


    override val connected = Property<Boolean>(false)
    protected val messageBroker = MessageBroker(scheduler)

    override fun advise(lifetime: Lifetime, id: RdId, handler: (InputStream) -> Unit) = adviseOn(lifetime, id, scheduler, handler)
    override fun adviseOn(lifetime: Lifetime, id: RdId, scheduler: IScheduler, handler: (InputStream) -> Unit) = messageBroker.adviseOn(lifetime, scheduler, id, handler)

    fun dumpToString() = messageBroker.dumpToString()
}