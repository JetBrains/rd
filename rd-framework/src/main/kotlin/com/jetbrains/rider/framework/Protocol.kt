package com.jetbrains.rider.framework

import com.jetbrains.rider.framework.base.RdExtBase
import com.jetbrains.rider.util.getLogger
import com.jetbrains.rider.util.reactive.IScheduler
import com.jetbrains.rider.util.reactive.ViewableSet
import com.jetbrains.rider.util.string.RName

class Protocol(
        override val serializers: ISerializers,
        override val identity: IIdentities,
        override val scheduler: IScheduler,
        override val wire: IWire //to initialize field with circular dependencies
) : IRdDynamic, IProtocol {

    override val location: RName = RName.Empty
    override val outOfSyncModels: ViewableSet<RdExtBase> = ViewableSet()

    companion object {
        val logCategory = "protocol"
        fun sublogger(subcategory: String) = getLogger("$logCategory.$subcategory")
        val initializationLogger = sublogger("INIT")
    }

    override val protocol: IProtocol get() = this
    override val serializationContext: SerializationCtx
        get() = SerializationCtx(serializers)
}
