package com.jetbrains.rd.framework

import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.framework.impl.InternId
import com.jetbrains.rd.framework.impl.ProtocolContexts
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.*
import com.jetbrains.rd.util.string.RName
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

/**
 * A node in a graph of entities that can be synchronized with its remote copy over a network or a similar connection.
 */
interface IRdDynamic {
    val protocol: IProtocol?
    val serializationContext: SerializationCtx?
    val location: RName
}

val IRdDynamic.protocolOrThrow: IProtocol get() = protocol ?: throw ProtocolNotBoundException(this.toString())

/**
 * A root node in an object graph which can be synchronized with its remote copy over a network or a similar connection.
 */
interface IProtocol : IRdDynamic {
    val name: String
    val serializers: ISerializers
    val identity: IIdentities
    val scheduler: IScheduler
    val wire: IWire
    val lifetime: Lifetime
    val isMaster : Boolean

    override val serializationContext: SerializationCtx
    override val protocol: IProtocol get() = this

    // Models for which the serialization hash does not match that on the other side
    val outOfSyncModels: ViewableSet<RdExtBase>

    val contexts : ProtocolContexts

    val extCreated: ISignal<ExtCreationInfoEx>

    fun <T: RdExtBase> getOrCreateExtension(clazz: KClass<T>, create: () -> T): T
    fun <T: RdExtBase> tryGetExtension(clazz: KClass<T>): T?
}

/**
 * Sends and receives serialized object data over a network or a similar connection.
 */
interface IWire {
    val connected: IPropertyView<Boolean>
    val heartbeatAlive: Property<Boolean>

    /**
     * Ping's interval.
     */
    var heartbeatIntervalMs: Long

    /**
     * Sends a data block with the given [id] and the given [writer] function that can write the data.
     */
    fun send(id: RdId, writer: (AbstractBuffer) -> Unit)

    /**
     * Adds a [entity] for receiving updated values of the object with the given [IRdWireable.rdid]. The handler is removed
     * when the given [lifetime] is terminated.
     */
    fun advise(lifetime: Lifetime, entity: IRdWireable)

    val contexts: ProtocolContexts

    fun setupContexts(newContexts: ProtocolContexts)
    
    fun tryGetById(rdId: RdId): IRdWireable?
}

interface IWireWithDelayedDelivery : IWire {
    fun startDeliveringMessages()
}

/**
 * Supports serializing and deserializing values of a specific type.
 */
interface ISerializer<T : Any?> {
    fun read (ctx : SerializationCtx, buffer: AbstractBuffer) : T
    fun write(ctx : SerializationCtx, buffer: AbstractBuffer, value : T)
}

/**
 * A serializer that can participate in polymorphic serialization.
 */
interface IMarshaller<T : Any> : ISerializer<T> {
    val _type: KClass<*>
    val id : RdId
        get() = RdId(_type.simpleName.getPlatformIndependentHash())
}

val IMarshaller<*>.fqn: String get() {
    return if (this is LazyCompanionMarshaller) this.fqn
    else _type.qualifiedName ?: _type.jvmName
}

class LazyCompanionMarshaller<T : Any>(
    override val id: RdId,
    val classLoader: ClassLoader,
    val fqn: String
) : IMarshaller<T> {
    companion object {
        private val possibleFields = listOf<String>("Companion", "INSTANCE")
    }
    private val lazy = lazy(LazyThreadSafetyMode.PUBLICATION) {
        val clazz = Class.forName(fqn, true, classLoader)
        val declaredFields = clazz.declaredFields

        declaredFields.firstOrNull { possibleFields.contains(it.name) }?.get(null) as? IMarshaller<T> ?: run {
            error("There are none of the fields ${possibleFields.joinToString()} in $clazz")
        }
    }

    override val _type: KClass<*>
        get() = lazy.value._type

    override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): T {
        return lazy.value.read(ctx, buffer)
    }

    override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: T) {
        lazy.value.write(ctx, buffer, value)
    }
}

interface IAbstractDeclaration<T> {
    fun readUnknownInstance(ctx: SerializationCtx, buffer: AbstractBuffer, unknownId: RdId, size: Int): T
}

interface IUnknownInstance {
    val unknownId: RdId
}

/**
 * A registry of known serializers.
 */
interface ISerializers {

    fun registerSerializersOwnerOnce(serializersOwner: ISerializersOwner)

    fun <T : Any> register(serializer: IMarshaller<T>)
    fun get(id: RdId): IMarshaller<*>?
    fun <T> readPolymorphicNullable(ctx: SerializationCtx, stream: AbstractBuffer, abstractDeclaration: IAbstractDeclaration<T>? = null): T?
    fun <T> writePolymorphicNullable(ctx: SerializationCtx, stream: AbstractBuffer, value: T)
    fun <T : Any> readPolymorphic(ctx: SerializationCtx, stream: AbstractBuffer, abstractDeclaration: IAbstractDeclaration<T>? = null): T
    fun <T : Any> writePolymorphic(ctx: SerializationCtx, stream: AbstractBuffer, value: T)
}

/**
 * Generates unique identifiers for objects in an object graph.
 *
 * Two types of IDs are supported:
 * - Dynamic IDs: Generated for entities created at runtime (via [next])
 * - Stable IDs: Hash-based IDs for statically known entities like extensions (via [mix])
 */
interface IIdentities {
    val dynamicKind : IdKind

    /**
     * Generates the next unique dynamic identifier for a runtime-created entity.
     */
    fun next(parent: RdId): RdId

    /**
     * Creates a stable identifier by mixing the parent ID with a string key.
     */
    fun mix(rdId: RdId, tail: String): RdId

    /**
     * Creates a stable identifier by mixing the parent ID with an integer key.
     */
    fun mix(rdId: RdId, tail: Int): RdId

    /**
     * Creates a stable identifier by mixing the parent ID with a long key.
     */
    fun mix(rdId: RdId, tail: Long): RdId
}

/**
 * Interns values sent over protocol
 */
interface IInternRoot<TBase : Any>: IRdReactive {
    /**
     * Returns an ID for a value. Returns invalid ID if the value was not interned
     */
    fun tryGetInterned(value: TBase): InternId

    /**
     * Interns a value and returns an ID for it. May return invalid ID if the value can't be interned due to multithreaded conflicts
     */
    fun intern(value: TBase): InternId

    /**
     * Gets a value from interned ID. Throws an exception if no value matches the given ID
     */
    fun <T : TBase> unIntern(id: InternId): T

    /**
     * Gets a valie from interned ID, returns null if no value matches the given ID
     */
    fun <T : TBase> tryUnIntern(id: InternId): T?

    /**
     * Removes interned value. Any future attempts to un-intern IDs previously associated with this value will fail.
     * Not thread-safe. It's up to user to ensure that the value being removed is not being used in messages written on background threads.
     */
    fun remove(value: TBase)
}
