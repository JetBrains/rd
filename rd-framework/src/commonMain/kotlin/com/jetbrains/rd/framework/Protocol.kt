package com.jetbrains.rd.framework

import com.jetbrains.rd.framework.base.RdExtBase
import com.jetbrains.rd.framework.impl.InternRoot
import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.reactive.ViewableSet
import com.jetbrains.rd.util.string.RName

class Protocol(
        override val serializers: ISerializers,
        override val identity: IIdentities,
        override val scheduler: IScheduler,
        override val wire: IWire, //to initialize field with circular dependencies
        val lifetime: Lifetime,
        serializationCtx: SerializationCtx? = null
) : IRdDynamic, IProtocol {

    override val location: RName = RName.Empty
    override val outOfSyncModels: ViewableSet<RdExtBase> = ViewableSet()

    companion object {
        val logCategory = "protocol"
        fun sublogger(subcategory: String) = getLogger("$logCategory.$subcategory")
        val initializationLogger = sublogger("INIT")
    }

    override val protocol: IProtocol get() = this
    override val serializationContext: SerializationCtx = serializationCtx ?: SerializationCtx(serializers, mapOf("Protocol" to InternRoot().also {
        it.rdid = RdId.Null.mix("ProtocolInternRoot")
        scheduler.queue {
            it.bind(lifetime, this, "ProtocolInternRoot")
        }
    }))
}
