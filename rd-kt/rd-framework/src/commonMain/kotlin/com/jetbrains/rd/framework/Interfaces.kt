package com.jetbrains.rd.framework

import com.jetbrains.rd.framework.base.IRdReactive
import com.jetbrains.rd.framework.base.IRdWireable
import com.jetbrains.rd.framework.base.ISerializersOwner
import com.jetbrains.rd.framework.base.RdExtBase
import com.jetbrains.rd.framework.impl.RdSet
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IPropertyView
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.reactive.ViewableSet
import com.jetbrains.rd.util.string.RName
import kotlin.reflect.KClass

/**
 * A node in a graph of entities that can be synchronized with its remote copy over a network or a similar connection.
 */
interface IRdDynamic {
    val protocol: IProtocol
    val serializationContext: SerializationCtx
    val location: RName
}

/**
 * A root node in an object graph which can be synchronized with its remote copy over a network or a similar connection.
 */
interface IProtocol : IRdDynamic {
    val name: String
    val serializers: ISerializers
    val identity: IIdentities
    val scheduler: IScheduler
    val wire: IWire
    val isMaster : Boolean

    // Models for which the serialization hash does not match that on the other side
    val outOfSyncModels: ViewableSet<RdExtBase>

    val clientIdSet: RdSet<ClientId>
}

/**
 * Sends and receives serialized object data over a network or a similar connection.
 */
interface IWire {
    val connected: IPropertyView<Boolean>

    /**
     * Sends a data block with the given [id] and the given [writer] function that can write the data.
     */
    fun send(id: RdId, writer: (AbstractBuffer) -> Unit)

    /**
     * Adds a [handler] for receiving updated values of the object with the given [id]. The handler is removed
     * when the given [lifetime] is terminated.
     */
    fun advise(lifetime: Lifetime, entity: IRdWireable)
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
    val toplevels : MutableSet<KClass<out ISerializersOwner>>

    fun <T : Any> register(serializer: IMarshaller<T>)
    fun <T> readPolymorphicNullable(ctx: SerializationCtx, stream: AbstractBuffer, abstractDeclaration: IAbstractDeclaration<T>? = null): T?
    fun <T> writePolymorphicNullable(ctx: SerializationCtx, stream: AbstractBuffer, value: T)
    fun <T : Any> readPolymorphic(ctx: SerializationCtx, stream: AbstractBuffer, abstractDeclaration: IAbstractDeclaration<T>? = null): T
    fun <T : Any> writePolymorphic(ctx: SerializationCtx, stream: AbstractBuffer, value: T)
}

/**
 * Generates unique identifiers for objects in an object graph.
 */
interface IIdentities {
    val dynamicKind : IdKind
    /**
     * Generates the next unique identifier.
     */
    fun next(parent: RdId): RdId
}

interface IInternRoot: IRdReactive {
    fun tryGetInterned(value: Any): Int
    fun internValue(value: Any): Int
    fun <T : Any> unInternValue(id: Int): T
    fun setInternedCorrespondence(id: Int, value: Any)
}
