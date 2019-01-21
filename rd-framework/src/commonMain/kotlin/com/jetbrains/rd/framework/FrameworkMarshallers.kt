package com.jetbrains.rd.framework

import com.jetbrains.rd.framework.impl.RdSecureString
import com.jetbrains.rd.util.Date
import com.jetbrains.rd.util.PublicApi
import com.jetbrains.rd.util.URI
import com.jetbrains.rd.util.UUID
import kotlin.reflect.*

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
    inline fun <reified T : Any> create(crossinline reader: (AbstractBuffer) -> T, crossinline writer: (AbstractBuffer, T) -> Unit, predefinedId: Int? = null): UniversalMarshaller<T> {
        return UniversalMarshaller(T::class, { _, stream -> reader(stream) }, { _, stream, v -> writer(stream, v) }, predefinedId)
    }
    

    inline fun <reified T : Enum<T>> enum(): UniversalMarshaller<T> {
        return create({ it.readEnum<T>() }, { stream, x -> stream.writeEnum(x) })
    }


    inline fun <TFrom : Any, reified TTo : Any> IMarshaller<TFrom>.delegate(crossinline to: (TFrom) -> TTo, crossinline from: (TTo) -> TFrom): IMarshaller<TTo> {
        return object : IMarshaller<TTo> {
            override val _type: KClass<*> get() = TTo::class
            override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): TTo = to(this@delegate.read(ctx, buffer))
            override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: TTo) = this@delegate.write(ctx, buffer, from(value))
        }
    }


    val Byte: IMarshaller<Byte> = create(AbstractBuffer::readByte, AbstractBuffer::writeByte, 1)
    val Int16: IMarshaller<Short> = create(AbstractBuffer::readShort, AbstractBuffer::writeShort, 2)
    val Int32: IMarshaller<Int> = create(AbstractBuffer::readInt, AbstractBuffer::writeInt, 3)
    val Int64: IMarshaller<Long> = create(AbstractBuffer::readLong, AbstractBuffer::writeLong, 4)
    val Float: IMarshaller<Float> = create(AbstractBuffer::readFloat, AbstractBuffer::writeFloat, 5)
    val Double: IMarshaller<Double> = create(AbstractBuffer::readDouble, AbstractBuffer::writeDouble, 6)
    val Char: IMarshaller<Char> = create(AbstractBuffer::readChar, AbstractBuffer::writeChar, 7)
    val Bool: IMarshaller<Boolean> = create(AbstractBuffer::readBoolean, AbstractBuffer::writeBoolean, 8)

    //empty
    val Void: IMarshaller<Unit> = create({ }, { _, _ -> }, 9)

    //normal string
    val String: UniversalMarshaller<String> = create(AbstractBuffer::readString, AbstractBuffer::writeString, 10)

    //aliases
    val Short = Int16
    val Int = Int32
    val Long = Int64


    //jvm-based
    val Guid: UniversalMarshaller<UUID> = create({ it.readUuid() }, AbstractBuffer::writeUuid, 11)
    val DateTime: UniversalMarshaller<Date> = create(AbstractBuffer::readDateTime, AbstractBuffer::writeDateTime, 12)
    val Uri: UniversalMarshaller<URI> = create({ it.readUri() }, AbstractBuffer::writeUri, 13)

    //rdId
    val RdId: IMarshaller<RdId> = create(AbstractBuffer::readRdId, AbstractBuffer::writeRdId, 14)

    //string for passwords
    val SecureString: IMarshaller<RdSecureString> = create({ RdSecureString(it.readString()) }, { buf, str -> buf.writeString(str.contents) }, 15)

    //arrays
    val ByteArray: UniversalMarshaller<ByteArray> = create(AbstractBuffer::readByteArray, AbstractBuffer::writeByteArray, 31)
    val CharArray: UniversalMarshaller<CharArray> = create(AbstractBuffer::readCharArray, AbstractBuffer::writeCharArray, 32)
    val ShortArray: UniversalMarshaller<ShortArray> = create(AbstractBuffer::readShortArray, AbstractBuffer::writeShortArray, 33)
    val IntArray: UniversalMarshaller<IntArray> = create(AbstractBuffer::readIntArray, AbstractBuffer::writeIntArray, 34)
    val LongArray: UniversalMarshaller<LongArray> = create(AbstractBuffer::readLongArray, AbstractBuffer::writeLongArray, 35)
    val FloatArray: UniversalMarshaller<FloatArray> = create(AbstractBuffer::readFloatArray, AbstractBuffer::writeFloatArray, 36)
    val DoubleArray: UniversalMarshaller<DoubleArray> = create(AbstractBuffer::readDoubleArray, AbstractBuffer::writeDoubleArray, 37)
    val BooleanArray: UniversalMarshaller<BooleanArray> = create(AbstractBuffer::readBooleanArray, AbstractBuffer::writeBooleanArray, 38)

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