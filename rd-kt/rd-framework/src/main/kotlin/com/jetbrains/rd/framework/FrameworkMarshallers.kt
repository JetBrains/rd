package com.jetbrains.rd.framework

import com.jetbrains.rd.framework.impl.RdSecureString
import com.jetbrains.rd.util.Date
import com.jetbrains.rd.util.EnumSet
import com.jetbrains.rd.util.PublicApi
import java.net.URI
import java.util.*
import kotlin.reflect.KClass
import kotlin.time.Duration

open class UniversalMarshaller<T : Any>(
        override val _type: KClass<*>,
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
open class DelegatedMarshaller<TFrom : Any, TTo : Any>(marshaller: IMarshaller<TFrom>, type: KClass<*>, to: (TFrom) -> TTo, from: (TTo) -> TFrom)
    : UniversalMarshaller<TTo>(type,
    reader = { ctx, stream -> to(marshaller.read(ctx, stream)) },
    writer = { ctx, stream, value -> marshaller.write(ctx, stream, from(value)) }
)

object FrameworkMarshallers {
    @Deprecated("Use an overload without inlining", ReplaceWith("create(T::class, reader, writer, predefinedId)", "com.jetbrains.rd.framework.FrameworkMarshallers.create"))
    inline fun <reified T : Any> create(noinline reader: (AbstractBuffer) -> T, noinline writer: (AbstractBuffer, T) -> Unit, predefinedId: Int? = null): UniversalMarshaller<T> {
        return create(T::class, reader, writer, predefinedId)
    }

    fun <T : Any> create(clazz: KClass<T>, reader: (AbstractBuffer) -> T, writer: (AbstractBuffer, T) -> Unit, predefinedId: Int? = null): UniversalMarshaller<T> {
        return UniversalMarshaller(clazz, { _, stream -> reader(stream) }, { _, stream, v -> writer(stream, v) }, predefinedId)
    }


    inline fun <reified T : Enum<T>> enum(): UniversalMarshaller<T> {
        return create(T::class, { it.readEnum<T>() }, { stream, x -> stream.writeEnum(x) } )
    }

    inline fun <reified T : Enum<T>> enumSet(): UniversalMarshaller<EnumSet<T>> {
        return create({ it.readEnumSet<T>() }, { stream, x -> stream.writeEnumSet(x) })
    }


    inline fun <TFrom : Any, reified TTo : Any> IMarshaller<TFrom>.delegate(crossinline to: (TFrom) -> TTo, crossinline from: (TTo) -> TFrom): IMarshaller<TTo> {
        return object : IMarshaller<TTo> {
            override val _type: KClass<*> get() = TTo::class
            override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): TTo = to(this@delegate.read(ctx, buffer))
            override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: TTo) = this@delegate.write(ctx, buffer, from(value))
        }
    }


    val Int8 : IMarshaller<Byte> = create(kotlin.Byte::class, AbstractBuffer::readByte, AbstractBuffer::writeByte, 1)
    val Int16: IMarshaller<Short> = create(kotlin.Short::class, AbstractBuffer::readShort, AbstractBuffer::writeShort, 2)
    val Int32: IMarshaller<Int> = create(kotlin.Int::class, AbstractBuffer::readInt, AbstractBuffer::writeInt, 3)
    val Int64: IMarshaller<Long> = create(kotlin.Long::class, AbstractBuffer::readLong, AbstractBuffer::writeLong, 4)

    val Float: IMarshaller<Float> = create(kotlin.Float::class, AbstractBuffer::readFloat, AbstractBuffer::writeFloat, 5)
    val Double: IMarshaller<Double> = create(kotlin.Double::class, AbstractBuffer::readDouble, AbstractBuffer::writeDouble, 6)
    val Char: IMarshaller<Char> = create(kotlin.Char::class, AbstractBuffer::readChar, AbstractBuffer::writeChar, 7)
    val Bool: IMarshaller<Boolean> = create(Boolean::class, AbstractBuffer::readBoolean, AbstractBuffer::writeBoolean, 8)

    //empty
    val Void: IMarshaller<Unit> = create(Unit::class, { }, { _, _ -> }, 9)

    //normal string
    val String: UniversalMarshaller<String> = create(kotlin.String::class, AbstractBuffer::readString, AbstractBuffer::writeString, 10)

    //aliases
    val Byte = Int8
    val Short = Int16
    val Int = Int32
    val Long = Int64


    //jvm-based
    val Guid: UniversalMarshaller<UUID> = create(java.util.UUID::class, { it.readUuid() }, AbstractBuffer::writeUuid, 11)
    val DateTime: UniversalMarshaller<Date> = create(java.util.Date::class, AbstractBuffer::readDateTime, AbstractBuffer::writeDateTime, 12)
    val Uri: UniversalMarshaller<URI> = create(java.net.URI::class, { it.readUri() }, AbstractBuffer::writeUri, 13)
    var TimeSpan: UniversalMarshaller<Duration> = create(Duration::class, AbstractBuffer::readTimeSpan, AbstractBuffer::writeTimeSpan, 14)

    //rdId
    val RdId: IMarshaller<RdId> = create(com.jetbrains.rd.framework.RdId::class, AbstractBuffer::readRdId, AbstractBuffer::writeRdId, 15)

    //string for passwords
    val SecureString: IMarshaller<RdSecureString> = create(RdSecureString::class, { RdSecureString(it.readString()) }, { buf, str -> buf.writeString(str.contents) }, 16)

    //arrays
    val ByteArray: UniversalMarshaller<ByteArray> = create(kotlin.ByteArray::class, AbstractBuffer::readByteArray, AbstractBuffer::writeByteArray, 31)
    val ShortArray: UniversalMarshaller<ShortArray> = create(kotlin.ShortArray::class, AbstractBuffer::readShortArray, AbstractBuffer::writeShortArray, 32)
    val IntArray: UniversalMarshaller<IntArray> = create(kotlin.IntArray::class, AbstractBuffer::readIntArray, AbstractBuffer::writeIntArray, 33)
    val LongArray: UniversalMarshaller<LongArray> = create(kotlin.LongArray::class, AbstractBuffer::readLongArray, AbstractBuffer::writeLongArray, 34)

    val FloatArray: UniversalMarshaller<FloatArray> = create(kotlin.FloatArray::class, AbstractBuffer::readFloatArray, AbstractBuffer::writeFloatArray, 35)
    val DoubleArray: UniversalMarshaller<DoubleArray> = create(kotlin.DoubleArray::class, AbstractBuffer::readDoubleArray, AbstractBuffer::writeDoubleArray, 36)

    val CharArray: UniversalMarshaller<CharArray> = create(kotlin.CharArray::class, AbstractBuffer::readCharArray, AbstractBuffer::writeCharArray, 37)
    val BooleanArray: UniversalMarshaller<BooleanArray> = create(kotlin.BooleanArray::class, AbstractBuffer::readBooleanArray, AbstractBuffer::writeBooleanArray, 38)


    //unsigned
    @ExperimentalUnsignedTypes
    val UByte : IMarshaller<UByte> = create(kotlin.UByte::class, AbstractBuffer::readUByte, AbstractBuffer::writeUByte, 41)
    @ExperimentalUnsignedTypes
    val UShort: IMarshaller<UShort> = create(kotlin.UShort::class, AbstractBuffer::readUShort, AbstractBuffer::writeUShort, 42)
    @ExperimentalUnsignedTypes
    val UInt: IMarshaller<UInt> = create(kotlin.UInt::class, AbstractBuffer::readUInt, AbstractBuffer::writeUInt, 43)
    @ExperimentalUnsignedTypes
    val ULong: IMarshaller<ULong> = create(kotlin.ULong::class, AbstractBuffer::readULong, AbstractBuffer::writeULong, 44)

    @ExperimentalUnsignedTypes
    val UByteArray: UniversalMarshaller<UByteArray> = create(kotlin.UByteArray::class, AbstractBuffer::readUByteArray, AbstractBuffer::writeUByteArray, 45)
    @ExperimentalUnsignedTypes
    val UShortArray: UniversalMarshaller<UShortArray> = create(kotlin.UShortArray::class, AbstractBuffer::readUShortArray, AbstractBuffer::writeUShortArray, 46)
    @ExperimentalUnsignedTypes
    val UIntArray: UniversalMarshaller<UIntArray> = create(kotlin.UIntArray::class, AbstractBuffer::readUIntArray, AbstractBuffer::writeUIntArray, 47)
    @ExperimentalUnsignedTypes
    val ULongArray: UniversalMarshaller<ULongArray> = create(kotlin.ULongArray::class, AbstractBuffer::readULongArray, AbstractBuffer::writeULongArray, 48)

    fun registerIn(serializers: ISerializers) {
        serializers.register(Int8)
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
        serializers.register(TimeSpan)
        serializers.register(Uri)
        serializers.register(RdId)
        serializers.register(SecureString)

        serializers.register(ByteArray)
        serializers.register(ShortArray)
        serializers.register(IntArray)
        serializers.register(LongArray)

        serializers.register(FloatArray)
        serializers.register(DoubleArray)
        serializers.register(CharArray)
        serializers.register(BooleanArray)
    }

}