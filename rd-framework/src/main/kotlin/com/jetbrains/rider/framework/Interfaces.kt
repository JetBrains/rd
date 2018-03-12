package com.jetbrains.rider.framework

import com.jetbrains.rider.framework.base.AbstractBuffer
import com.jetbrains.rider.framework.base.ISerializersOwner
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.reactive.IPropertyView
import com.jetbrains.rider.util.reactive.IScheduler

/**
 * A node in a graph of entities that can be synchronized with its remote copy over a network or a similar connection.
 */
interface IRdDynamic {
    val protocol: IProtocol
    val serializationContext: SerializationCtx
}

/**
 * A root node in an object graph which can be synchronized with its remote copy over a network or a similar connection.
 */
interface IProtocol : IRdDynamic {
    val serializers: ISerializers
    val identity: IIdentities
    val scheduler: IScheduler
    val wire: IWire
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
    fun advise(lifetime: Lifetime, id: RdId, handler: (AbstractBuffer) -> Unit)

    /**
     * Adds a [handler] for receiving updated values of the object with the given [id] and handling them through the
     * specified [scheduler]. The handler is removed when the given [lifetime] is terminated.
     */
    fun adviseOn(lifetime: Lifetime, id: RdId, scheduler: IScheduler, handler: (AbstractBuffer) -> Unit)
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
    val _type: Class<*>
    val id : RdId
        get() = RdId(_type.simpleName.getPlatformIndependentHash())
}

/**
 * A registry of known serializers.
 */
interface ISerializers {
    val toplevels : MutableSet<Class<ISerializersOwner>>

    fun <T: Any> register(serializer: IMarshaller<T>)
    fun <T> readPolymorphicNullable(ctx: SerializationCtx, stream: AbstractBuffer): T?
    fun <T> writePolymorphicNullable(ctx: SerializationCtx, stream: AbstractBuffer, value: T)
    fun <T : Any> readPolymorphic(ctx: SerializationCtx, stream: AbstractBuffer): T
    fun <T : Any> writePolymorphic(ctx: SerializationCtx, stream: AbstractBuffer, value: T)
}

/**
 * Generates unique identifiers for objects in an object graph.
 */
interface IIdentities {
    /**
     * Generates the next unique identifier.
     */
    fun next(parent: RdId): RdId
}

interface IInternRoot {
    fun tryGetInterned(value: Any): Int
    fun internValue(value: Any): Int
    fun <T : Any> unInternValue(id: Int): T
    fun setInternedCorrespondence(id: Int, value: Any)
}
