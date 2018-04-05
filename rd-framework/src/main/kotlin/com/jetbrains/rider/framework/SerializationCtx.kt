package com.jetbrains.rider.framework

import com.jetbrains.rider.framework.impl.InternRoot

class SerializationCtx(val serializers: ISerializers, val internRoot: IInternRoot? = null) {
    constructor(protocol: IProtocol) : this(protocol.serializers)
}

object Polymorphic : ISerializer<Any?> {
    @Suppress("UNCHECKED_CAST")
    operator fun <T> invoke() : ISerializer<T> = this as ISerializer<T>
    override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): Any? = ctx.serializers.readPolymorphicNullable(ctx, buffer)
    override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value:Any?) = ctx.serializers.writePolymorphicNullable(ctx, buffer, value)
}

fun <T> ISerializer<T>.list() : ISerializer<List<T>> = object : ISerializer<List<T>> {
    override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): List<T> = buffer.readList { this@list.read(ctx, buffer) }
    override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: List<T>) = buffer.writeList(value) { this@list.write(ctx, buffer, it)}
}

fun <T> ISerializer<T>.array() : ISerializer<Array<T>> = object : ISerializer<Array<T>> {
    override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): Array<T> = buffer.readArray { this@array.read(ctx, buffer) }
    override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: Array<T>) = buffer.writeArray(value) { this@array.write(ctx, buffer, it)}
}

fun <T : Any> ISerializer<T>.nullable() : ISerializer<T?> = object : ISerializer<T?> {
    override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): T? = buffer.readNullable { this@nullable.read(ctx, buffer) }
    override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: T?) = buffer.writeNullable(value) { this@nullable.write(ctx, buffer, it)}
}

fun <T: Any> ISerializer<T>.interned() : ISerializer<T> = object : ISerializer<T> {
    override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): T = ctx.readInterned(buffer, this@interned::read)
    override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: T) = ctx.writeInterned(buffer, value, this@interned::write)
}

fun SerializationCtx.withInternRootHere(isMaster: Boolean): SerializationCtx {
    return SerializationCtx(serializers, InternRoot(isMaster))
}

inline fun <T: Any> SerializationCtx.readInterned(stream: AbstractBuffer, readValueDelegate: (SerializationCtx, AbstractBuffer) -> T): T {
    val interningRoot = internRoot ?: return readValueDelegate(this, stream)
    val hasValue = stream.readBoolean()
    return if (hasValue) {
        val value = readValueDelegate(this, stream)
        val id = stream.readInt()
        interningRoot.setInternedCorrespondence(id, value)
        value
    } else {
        interningRoot.unInternValue(stream.readInt())
    }
}

inline fun <T: Any> SerializationCtx.writeInterned(stream: AbstractBuffer, value: T, writeValueDelegate: (SerializationCtx, AbstractBuffer, T) -> Unit) {
    val interningRoot = internRoot ?: return writeValueDelegate(this, stream, value)
    var alreadyInternedId = interningRoot.tryGetInterned(value)
    val isNewValue = alreadyInternedId < 0
    stream.writeBoolean(isNewValue)
    if (isNewValue) {
        writeValueDelegate(this, stream, value)
        alreadyInternedId = interningRoot.internValue(value)
    }
    stream.writeInt(alreadyInternedId)
}