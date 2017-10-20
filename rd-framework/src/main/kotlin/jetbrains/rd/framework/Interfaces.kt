package com.jetbrains.rider.framework

import com.jetbrains.rider.util.getPlatformIndependentHash
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.reactive.IProperty
import com.jetbrains.rider.util.reactive.IScheduler
import java.io.InputStream
import java.io.OutputStream

enum class OriginKind {
    Local,
    Remote
}

interface  IRdDynamic {
    val protocol : IProtocol
    val serializationContext: SerializationCtx
}

interface IProtocol : IRdDynamic {
    val serializers : ISerializers
    val identity : IIdentities
    val scheduler: IScheduler
    val wire : IWire
}

interface IWire {
    val connected: IProperty<Boolean>

    fun send(id: RdId, writer: (OutputStream) -> Unit)
    fun advise(lifetime: Lifetime, id: RdId, handler: (InputStream) -> Unit)
    fun adviseOn(lifetime: Lifetime, id: RdId, scheduler: IScheduler, handler: (InputStream) -> Unit)
}

//data class Serializer<T>(val read: (SerializationCtx, InputStream) -> T, val write: (SerializationCtx, OutputStream , T) -> Unit) {
//    companion object {
//        @Suppress("UNCHECKED_CAST")
//        fun <T> polymorphic() : Serializer<T> = default as Serializer<T>
//
//        val default = Serializer(
//            {ctx, stream -> ctx.serializers.readPolymorphic<Any?>(ctx, stream)},
//            {ctx, stream, value -> ctx.serializers.writePolymorphic(ctx, stream, value) }
//        )
//
//        fun<T : Any> fromMarshaller(marshaller : IMarshaller<T>) : Serializer<T> = Serializer<T>(
//            {ctx, stream -> marshaller.read(ctx, stream)},
//            {ctx, stream, value -> marshaller.write(ctx, stream, value)}
//            )
//    }
//}

interface ISerializer<T : Any?> {
    fun read (ctx : SerializationCtx, stream : InputStream) : T
    fun write(ctx : SerializationCtx, stream : OutputStream ,  value : T)
}

interface IMarshaller<T : Any> : ISerializer<T> {
    val _type: Class<*>
    val id : RdId
        get() = RdId(IdKind.StaticType, _type.simpleName.getPlatformIndependentHash())
}


interface ISerializers {
    val toplevels : MutableSet<Class<*>>

    fun <T: Any> register(serializer: IMarshaller<T>)
    fun <T> readPolymorphic(ctx : SerializationCtx, stream : InputStream) : T
    fun <T> writePolymorphic(ctx : SerializationCtx, stream : OutputStream, value : T)
}

interface IIdentities {
    operator fun next() : RdId
}

interface IInternRoot {
    fun tryGetInterned(value: Any): Int
    fun internValue(value: Any): Int
    fun <T : Any> unInternValue(id: Int): T
    fun setInternedCorrespondence(id: Int, value: Any)
}
