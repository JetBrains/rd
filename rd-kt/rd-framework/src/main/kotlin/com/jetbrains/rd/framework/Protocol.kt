package com.jetbrains.rd.framework

import com.jetbrains.rd.framework.base.RdExtBase
import com.jetbrains.rd.framework.impl.InternRoot
import com.jetbrains.rd.framework.impl.ProtocolContexts
import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.reactive.ViewableSet
import com.jetbrains.rd.util.string.RName


class Protocol internal constructor(
    override val name: String,
    override val serializers: ISerializers,
    override val identity: IIdentities,
    override val scheduler: IScheduler,
    override val wire: IWire, //to initialize field with circular dependencies
    val lifetime: Lifetime,
    serializationCtx: SerializationCtx? = null,
    parentContexts: ProtocolContexts? = null,
    vararg initialContexts: RdContext<*>
) : IRdDynamic, IProtocol {

    @Deprecated("Backward compatible implementation for AWS plugin compile against Rider SDK 2019.2")
    constructor(serializers: ISerializers,
                identity: IIdentities,
                scheduler: IScheduler,
                wire: IWire,
                lifetime: Lifetime) : this("Noname-Please-Specify-Name", serializers, identity, scheduler, wire, lifetime)

    constructor(name: String,
                serializers: ISerializers,
                identity: IIdentities,
                scheduler: IScheduler,
                wire: IWire, //to initialize field with circular dependencies
                lifetime: Lifetime,
                vararg initialContexts: RdContext<*>) : this(name, serializers, identity, scheduler, wire, lifetime, null, null, *initialContexts)

    override val location: RName = RName(name)
    override val outOfSyncModels: ViewableSet<RdExtBase> = ViewableSet()

    override val isMaster: Boolean = identity.dynamicKind == IdKind.Client

    companion object {
        val logCategory = "protocol"
        fun sublogger(subcategory: String) = getLogger("$logCategory.$subcategory")
        val initializationLogger = sublogger("INIT")
    }

    override val protocol: IProtocol get() = this
    override val serializationContext: SerializationCtx = serializationCtx ?: SerializationCtx(serializers, mapOf("Protocol" to InternRoot<Any>().also {
        it.rdid = RdId.Null.mix("ProtocolInternRoot")
    }))

    override val contexts: ProtocolContexts = parentContexts ?: ProtocolContexts(serializationContext)

    init {
        wire.setupContexts(contexts)

        if(serializationCtx == null) {
            serializationContext.internRoots.getValue("Protocol").bind(lifetime, this, "ProtocolInternRoot")
        }

        initialContexts.forEach {
            contexts.registerContext(it)
        }

        if (parentContexts == null) {
            contexts.also {
                it.rdid = RdId.Null.mix("ProtocolContextHandler")
                scheduler.invokeOrQueue {
                    it.bind(lifetime, this, "ProtocolContextHandler")
                }
            }
        }

        if (wire is IWireWithDelayedDelivery)
            wire.startDeliveringMessages()
    }
}
