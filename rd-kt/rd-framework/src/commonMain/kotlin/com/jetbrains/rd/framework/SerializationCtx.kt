package com.jetbrains.rd.framework

import com.jetbrains.rd.framework.base.RdBindableBase
import com.jetbrains.rd.framework.impl.InternRoot
import com.jetbrains.rd.framework.impl.readInternId
import com.jetbrains.rd.framework.impl.writeInternId

class SerializationCtx(val serializers: ISerializers, val internRoots: Map<String, IInternRoot<Any>> = emptyMap()) {
    constructor(protocol: IProtocol) : this(protocol.serializers)
}

object Polymorphic : ISerializer<Any?> {
    @Suppress("UNCHECKED_CAST")
    operator fun <T> invoke() : ISerializer<T> = this as ISerializer<T>

    override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): Any? =
        ctx.serializers.readPolymorphicNullable(ctx, buffer)

    override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: Any?) =
        ctx.serializers.writePolymorphicNullable(ctx, buffer, value)
}

@Suppress("UNCHECKED_CAST")
class AbstractPolymorphic<T>(val declaration: IAbstractDeclaration<T>) : ISerializer<T> {
    @Suppress("UNCHECKED_CAST")
    operator fun <T> invoke() : ISerializer<T> = this as ISerializer<T>

    override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): T =
        ctx.serializers.readPolymorphicNullable(ctx, buffer, declaration) as T

    override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: T) =
        ctx.serializers.writePolymorphicNullable(ctx, buffer, value)
}

fun <T> ISerializer<T>.list() : ISerializer<List<T>> = object : ISerializer<List<T>> {
    override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): List<T> = buffer.readList { this@list.read(ctx, buffer) }
    override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: List<T>) = buffer.writeList(value) { this@list.write(ctx, buffer, it)}
}

inline fun <reified T> ISerializer<T>.array() : ISerializer<Array<T>> = object : ISerializer<Array<T>> {
    override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): Array<T> = buffer.readArray { this@array.read(ctx, buffer) }
    override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: Array<T>) = buffer.writeArray(value) { this@array.write(ctx, buffer, it)}
}

fun <T : Any> ISerializer<T>.nullable() : ISerializer<T?> = object : ISerializer<T?> {
    override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): T? = buffer.readNullable { this@nullable.read(ctx, buffer) }
    override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: T?) = buffer.writeNullable(value) { this@nullable.write(ctx, buffer, it)}
}

fun <T: Any> ISerializer<T>.interned(internKey: String) : ISerializer<T> = object : ISerializer<T> {
    override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): T = ctx.readInterned(buffer, internKey, this@interned::read)
    override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: T) = ctx.writeInterned(buffer, value, internKey, this@interned::write)
}

fun SerializationCtx.withInternRootsHere(owner: RdBindableBase, vararg newRoots: String): SerializationCtx {
    return SerializationCtx(serializers, internRoots.plus(newRoots.associate {
        it to owner.getOrCreateHighPriorityExtension("InternRoot-$it") { InternRoot<Any>() }.apply { rdid = owner.rdid.mix(".InternRoot-$it") } }))
}

inline fun <T: Any> SerializationCtx.readInterned(stream: AbstractBuffer, internKey: String, readValueDelegate: (SerializationCtx, AbstractBuffer) -> T): T {
    val internRoot = internRoots[internKey] ?: return readValueDelegate(this, stream)
    val internId = stream.readInternId()
    return if (internId.isValid)
        internRoot.unIntern(internId)
    else
        readValueDelegate(this, stream)
}

inline fun <T: Any> SerializationCtx.writeInterned(stream: AbstractBuffer, value: T, internKey: String, writeValueDelegate: (SerializationCtx, AbstractBuffer, T) -> Unit) {
    val internRoot = internRoots[internKey] ?: return writeValueDelegate(this, stream, value)
    val internId = internRoot.intern(value)
    stream.writeInternId(internId)
    if (!internId.isValid) // value couldn't be interned, send as-is
        writeValueDelegate(this, stream, value)
}