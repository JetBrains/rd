package com.jetbrains.rider.framework

import com.jetbrains.rider.util.getLogger
import com.jetbrains.rider.util.reactive.IScheduler

class Protocol(
        override val serializers: ISerializers,
        override val identity: IIdentities,
        override val scheduler: IScheduler,
        override val wire: IWire //to initialize field with circular dependencies
) : IRdDynamic, IProtocol {

    companion object {
        val logCategory = "protocol"
        fun sublogger(subcategory : String) = getLogger("$logCategory.$subcategory")
        val initializationLogger = sublogger("INIT")
    }

    override val protocol: IProtocol get() = this
    override val serializationContext: SerializationCtx
        get() = SerializationCtx(serializers)
}
