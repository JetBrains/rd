package com.jetbrains.rider.framework

import com.jetbrains.rider.framework.base.AbstractBuffer
import com.jetbrains.rider.framework.impl.RdSecureString
import com.jetbrains.rider.util.PublicApi
import java.net.URI
import java.util.*


open class UniversalMarshaller<T : Any>(
    override val _type: Class<*>,
    val reader: (SerializationCtx, AbstractBuffer) -> T,
    val writer: (SerializationCtx, AbstractBuffer, T) -> Unit,
    val predefinedId: Int? = null)
    : IMarshaller<T> {

    override fun read(ctx: SerializationCtx, buffer: AbstractBuffer) = reader(ctx, buffer)
    override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: T) = writer(ctx, buffer, value)
    override val id: RdId
        get() = predefinedId?.let { RdId(it.toLong()) } ?: super.id
}


@PublicApi
@Suppress("unused")
open class DelegatedMarshaller<TFrom : Any, TTo : Any>(marshaller: IMarshaller<TFrom>, type: Class<*>, to: (TFrom) -> TTo, from: (TTo) -> TFrom)
    : UniversalMarshaller<TTo>(type,
    reader = { ctx, stream -> to(marshaller.read(ctx, stream)) },
    writer = { ctx, stream, value -> marshaller.write(ctx, stream, from(value)) }
)

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
object FrameworkMarshallers {
    fun <T : Any> create(type: Class<*>, reader: (AbstractBuffer) -> T, writer: (AbstractBuffer, T) -> Unit, predefinedId: Int? = null): UniversalMarshaller<T> {
        return UniversalMarshaller(type, { ctx, stream -> reader(stream) }, { ctx, stream, v -> writer(stream, v) }, predefinedId)
    }
    

    inline fun <reified T : Enum<T>> enum(): UniversalMarshaller<T> {
        return create(T::class.java, { it.readEnum<T>() }, { stream, x -> stream.writeEnum(x) })
    }


    inline fun <TFrom : Any, reified TTo : Any> IMarshaller<TFrom>.delegate(crossinline to: (TFrom) -> TTo, crossinline from: (TTo) -> TFrom): IMarshaller<TTo> {
        return object : IMarshaller<TTo> {
            override val _type: Class<*> get() = TTo::class.java
            override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): TTo = to(this@delegate.read(ctx, buffer))
            override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: TTo) = this@delegate.write(ctx, buffer, from(value))
        }
    }


    val Byte: IMarshaller<Byte> = create(java.lang.Byte::class.java, AbstractBuffer::readByte, AbstractBuffer::writeByte, 1)
    val Int16: IMarshaller<Short> = create(java.lang.Short::class.java, AbstractBuffer::readShort, AbstractBuffer::writeShort, 2)
    val Int32: IMarshaller<Int> = create(java.lang.Integer::class.java, AbstractBuffer::readInt, AbstractBuffer::writeInt, 3)
    val Int64: IMarshaller<Long> = create(java.lang.Long::class.java, AbstractBuffer::readLong, AbstractBuffer::writeLong, 4)
    val Float: IMarshaller<Float> = create(java.lang.Float::class.java, AbstractBuffer::readFloat, AbstractBuffer::writeFloat, 5)
    val Double: IMarshaller<Double> = create(java.lang.Double::class.java, AbstractBuffer::readDouble, AbstractBuffer::writeDouble, 6)
    val Char: IMarshaller<Char> = create(java.lang.Character::class.java, AbstractBuffer::readChar, AbstractBuffer::writeChar, 7)
    val Bool: IMarshaller<Boolean> = create(java.lang.Boolean::class.java, AbstractBuffer::readBoolean, AbstractBuffer::writeBoolean, 8)

    //special
    val Void: IMarshaller<Unit> = create(Unit::class.java, { }, { _, _ -> }, 9)

    //aliases
    val Short = Int16
    val Int = Int32
    val Long = Int64

    val String: UniversalMarshaller<String> = create(kotlin.String::class.java, AbstractBuffer::readString, AbstractBuffer::writeString, 10)
    val Guid: UniversalMarshaller<UUID> = create(UUID::class.java, { it.readUuid() }, AbstractBuffer::writeUuid, 11)
    val DateTime: UniversalMarshaller<Date> = create(java.util.Date::class.java, AbstractBuffer::readDateTime, AbstractBuffer::writeDateTime, 12)
    val Uri: UniversalMarshaller<URI> = create(URI::class.java, { it.readUri() }, AbstractBuffer::writeUri, 13)
    val RdId: IMarshaller<RdId> = create(com.jetbrains.rider.framework.RdId::class.java, AbstractBuffer::readRdId, AbstractBuffer::writeRdId, 14)

    val SecureString: IMarshaller<RdSecureString> = create(RdSecureString::class.java, { RdSecureString(it.readString()) }, { buf, str -> buf.writeString(str.contents) }, 15)

    val ByteArray: UniversalMarshaller<ByteArray> = create(kotlin.ByteArray::class.java, AbstractBuffer::readByteArray, AbstractBuffer::writeByteArray, 31)
    val CharArray: UniversalMarshaller<CharArray> = create(kotlin.CharArray::class.java, AbstractBuffer::readCharArray, AbstractBuffer::writeCharArray, 32)
    val ShortArray: UniversalMarshaller<ShortArray> = create(kotlin.ShortArray::class.java, AbstractBuffer::readShortArray, AbstractBuffer::writeShortArray, 33)
    val IntArray: UniversalMarshaller<IntArray> = create(kotlin.IntArray::class.java, AbstractBuffer::readIntArray, AbstractBuffer::writeIntArray, 34)
    val LongArray: UniversalMarshaller<LongArray> = create(kotlin.LongArray::class.java, AbstractBuffer::readLongArray, AbstractBuffer::writeLongArray, 35)
    val FloatArray: UniversalMarshaller<FloatArray> = create(kotlin.FloatArray::class.java, AbstractBuffer::readFloatArray, AbstractBuffer::writeFloatArray, 36)
    val DoubleArray: UniversalMarshaller<DoubleArray> = create(kotlin.DoubleArray::class.java, AbstractBuffer::readDoubleArray, AbstractBuffer::writeDoubleArray, 37)
    val BooleanArray: UniversalMarshaller<BooleanArray> = create(kotlin.BooleanArray::class.java, AbstractBuffer::readBooleanArray, AbstractBuffer::writeBooleanArray, 38)

    fun registerIn(serializers: ISerializers) {
        serializers.register(Byte)
        serializers.register(Int16)
        serializers.register(Int32)
        serializers.register(Int64)
        serializers.register(Float)
        serializers.register(Double)
        serializers.register(Char)
        serializers.register(Bool)
        serializers.register(Void)


        serializers.register(String)
        serializers.register(Guid) // We use Microsoft GUID Structure for marshalling, see https://en.wikipedia.org/wiki/Globally_unique_identifier#Binary_encoding
        serializers.register(DateTime) // We use .NET DateTime.Ticks for marshalling, see http://stackoverflow.com/questions/3706306/c-sharp-datetime-ticks-equivalent-in-java/3706320#3706320
        serializers.register(Uri)
        serializers.register(RdId)
        serializers.register(SecureString)

        serializers.register(ByteArray)
        serializers.register(CharArray)
        serializers.register(ShortArray)
        serializers.register(IntArray)
        serializers.register(LongArray)
        serializers.register(FloatArray)
        serializers.register(DoubleArray)
        serializers.register(BooleanArray)
    }

}