package com.jetbrains.rider.framework

import com.jetbrains.rider.util.Maybe
import com.jetbrains.rider.util.reactive.IScheduler
import org.apache.commons.logging.LogFactory

class Protocol(
        override val serializers: ISerializers,
        override val identity: IIdentities,
        override val scheduler: IScheduler,
        wireInitializer : (Protocol) -> IWire //to initialize field with circular dependencies
) : IRdDynamic, IProtocol {

    companion object {
        val logCategory = "protocol"
        fun sublogger(subcategory : String) = Maybe.Just(LogFactory.getLog("$logCategory.$subcategory")!!)
        val initializationLogger = sublogger("INIT")
    }

    override val protocol: IProtocol get() = this
    override val wire: IWire = wireInitializer(this)
    override var serializationContext: SerializationCtx
        get() = SerializationCtx(this)
        set(value) = throw UnsupportedOperationException("Can't set serialization context on protocol")
}
