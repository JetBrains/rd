package com.jetbrains.rider.framework

import com.jetbrains.rider.framework.impl.InternRoot
import java.io.InputStream
import java.io.OutputStream

class SerializationCtx(val protocol: IProtocol, val internRoot: IInternRoot? = null) {
    val serializers: ISerializers get() = protocol.serializers
}

object Polymorphic : ISerializer<Any?> {
    @Suppress("UNCHECKED_CAST")
    operator fun <T> invoke() : ISerializer<T> = this as ISerializer<T>
    override fun read(ctx: SerializationCtx, stream: InputStream): Any? = ctx.serializers.readPolymorphic(ctx, stream)
    override fun write(ctx: SerializationCtx, stream: OutputStream, value:Any?) = ctx.serializers.writePolymorphic(ctx, stream, value)
}

inline fun <reified T> ISerializer<T>.list() : ISerializer<List<T>> = object : ISerializer<List<T>> {
    override fun read(ctx: SerializationCtx, stream: InputStream): List<T> = stream.readList { this@list.read(ctx, stream) }
    override fun write(ctx: SerializationCtx, stream: OutputStream, value: List<T>) = stream.writeList(value) {elem -> this@list.write(ctx, stream, elem)}
}

inline fun <reified T> ISerializer<T>.array() : ISerializer<Array<T>> = object : ISerializer<Array<T>> {
    override fun read(ctx: SerializationCtx, stream: InputStream): Array<T> = stream.readArray { this@array.read(ctx, stream) }
    override fun write(ctx: SerializationCtx, stream: OutputStream, value: Array<T>) = stream.writeArray(value) {elem -> this@array.write(ctx, stream, elem)}
}

inline fun <reified T : Any> ISerializer<T>.nullable() : ISerializer<T?> = object : ISerializer<T?> {
    override fun read(ctx: SerializationCtx, stream: InputStream): T? = stream.readNullable { this@nullable.read(ctx, stream) }
    override fun write(ctx: SerializationCtx, stream: OutputStream, value: T?) = stream.writeNullable(value) {elem -> this@nullable.write(ctx, stream, elem)}
}

inline fun <reified T: Any> ISerializer<T>.interned() : ISerializer<T> = object : ISerializer<T> {
    override fun read(ctx: SerializationCtx, stream: InputStream): T = ctx.readInterned(stream, this@interned::read)
    override fun write(ctx: SerializationCtx, stream: OutputStream, value: T) = ctx.writeInterned(stream, value, this@interned::write)
}

fun SerializationCtx.withInternRootHere(isMaster: Boolean): SerializationCtx {
    return SerializationCtx(protocol, InternRoot(isMaster))
}

inline fun <reified T: Any> SerializationCtx.readInterned(stream: InputStream, readValueDelegate: (SerializationCtx, InputStream) -> T): T {
    val interningRoot = internRoot ?: return readValueDelegate(this, stream)
    val hasValue = stream.readBool()
    return if (hasValue) {
        val value = readValueDelegate(this, stream)
        val id = stream.readInt()
        interningRoot.setInternedCorrespondence(id, value)
        value
    } else {
        interningRoot.unInternValue(stream.readInt())
    }
}

inline fun <reified T: Any> SerializationCtx.writeInterned(stream: OutputStream, value: T, writeValueDelegate: (SerializationCtx, OutputStream, T) -> Unit) {
    val interningRoot = internRoot ?: return writeValueDelegate(this, stream, value)
    var alreadyInternedId = interningRoot.tryGetInterned(value)
    val isNewValue = alreadyInternedId < 0
    stream.writeBool(isNewValue)
    if (isNewValue) {
        writeValueDelegate(this, stream, value)
        alreadyInternedId = interningRoot.internValue(value)
    }
    stream.writeInt(alreadyInternedId)
}