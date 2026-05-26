package com.jetbrains.rd.framework

import com.jetbrains.rd.framework.base.AllowBindingCookie
import com.jetbrains.rd.framework.base.RdExtBase
import com.jetbrains.rd.framework.base.bindTopLevel
import com.jetbrains.rd.framework.impl.InternRoot
import com.jetbrains.rd.framework.impl.ProtocolContexts
import com.jetbrains.rd.framework.impl.RdSignal
import com.jetbrains.rd.util.Sync
import com.jetbrains.rd.util.collections.SynchronizedSet
import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.*
import com.jetbrains.rd.util.string.RName
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass


class Protocol internal constructor(
    override val name: String,
    override val serializers: ISerializers,
    override val identity: IIdentities,
    override val scheduler: IScheduler,
    override val wire: IWire, //to initialize field with circular dependencies
    override val lifetime: Lifetime,
    private val parentProtocol: Protocol?,
    parentExtConfirmation: RdSignal<ExtCreationInfo>? = null,
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
    override val outOfSyncModels: ViewableSet<RdExtBase> = ViewableSet(SynchronizedSet())

    override val isMaster: Boolean = identity.dynamicKind == IdKind.Client

    val rdEntitiesRegistrar: RdEntitiesRegistrar = parentProtocol?.rdEntitiesRegistrar ?: RdEntitiesRegistrar()

    companion object {
        val logCategory = "protocol"
        fun sublogger(subcategory: String) = getLogger("$logCategory.$subcategory")
        val initializationLogger = sublogger("INIT")
    }

    override val protocol: IProtocol get() = this
    override val serializationContext: SerializationCtx = parentProtocol?.serializationContext ?: SerializationCtx(serializers, identity, mapOf("Protocol" to InternRoot<Any>().also {
        it.rdid = identity.mix(RdId.Null, "ProtocolInternRoot")
    }))

    override val contexts: ProtocolContexts = parentProtocol?.contexts ?: ProtocolContexts(serializationContext)

    override val extCreated: ISignal<ExtCreationInfoEx>

    private val extConfirmation: RdSignal<ExtCreationInfo>
    private val extIsLocal: ThreadLocal<Boolean>

    private val extensionPerClassLocks = ConcurrentHashMap<KClass<*>, Any>()
    private val extensions = ConcurrentHashMap<KClass<*>, Any>()

    init {
        wire.setupContexts(contexts)

        if(parentProtocol?.serializationContext == null) {
            serializationContext.internRoots.getValue("Protocol").bindTopLevel(lifetime, this, "ProtocolInternRoot")
        }

        initialContexts.forEach {
            contexts.registerContext(it)
        }

        if (parentProtocol?.contexts == null) {
            contexts.also {
                it.rdid = identity.mix(RdId.Null, "ProtocolContextHandler")
                AllowBindingCookie.allowBind {
                    it.bindTopLevel(lifetime, this, "ProtocolContextHandler")
                }
            }
        }

        extCreated = parentProtocol?.extCreated ?: Signal()
        extConfirmation = parentExtConfirmation ?: createExtSignal(identity).also { signal ->
            val protocolScheduler = scheduler
            signal.scheduler = (protocolScheduler as? ISchedulerWithBackground)?.backgroundScheduler ?: protocolScheduler
        }
        extIsLocal = ThreadLocal.withInitial { false }
        AllowBindingCookie.allowBind {
            extConfirmation.bindTopLevel(lifetime, this, "ProtocolExtCreated")
            extConfirmation.advise(lifetime) { info ->
                // triggered both from local and remote sides
                extCreated.fire(ExtCreationInfoEx(info, extIsLocal.get()))
            }
        }

        if (wire is IWireWithDelayedDelivery)
            wire.startDeliveringMessages()
    }

    internal fun submitExtCreated(info: ExtCreationInfo) {
        require(extIsLocal.get() == false) { "!extIsLocal" }
        extIsLocal.set(true)
        try {
            extConfirmation.fire(info)
        } finally {
            extIsLocal.set(false)
        }
    }

    override fun <T: RdExtBase> getOrCreateExtension(clazz: KClass<T>, create: () -> T): T {
        parentProtocol?.let {
            return it.getOrCreateExtension(clazz, create)
        }

        val lock = extensionPerClassLocks.getOrPut(clazz) { Any() }
        Sync.lock(lock) {
            val res = extensions[clazz] ?: run {
                val newExtension = create()
                extensions[clazz] = newExtension
                val declName = clazz.simpleName ?: error("Can't get simple name for class $clazz")
                newExtension.identify(identity, identity.mix(RdId.Null, declName))
                newExtension.bindTopLevel(lifetime, this, declName)
                newExtension
            }
            return castExtension(res, clazz)
        }
    }

    override fun <T : RdExtBase> tryGetExtension(clazz: KClass<T>): T? {
        parentProtocol?.let {
            return it.tryGetExtension(clazz)
        }

        val lock = extensionPerClassLocks.getOrPut(clazz) { Any() }
        Sync.lock(lock) {
            val res = extensions[clazz] ?: return null
            return castExtension(res, clazz)
        }
    }
    
    private fun <T: Any> castExtension(value: Any, clazz: KClass<T>): T {
        @Suppress("UNCHECKED_CAST")
        return value as? T
            ?: error("Wrong class found in top level extension, expected `${clazz.simpleName}` but found `${value::class.simpleName}`")  
    } 
}
