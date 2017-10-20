package com.jetbrains.rider.framework

import com.jetbrains.rider.framework.impl.IRdRpc
import com.jetbrains.rider.framework.impl.RdMap
import com.jetbrains.rider.framework.impl.RdProperty
import com.jetbrains.rider.framework.impl.RdSignal
import com.jetbrains.rider.util.PublicApi
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.util.*


open class UniversalMarshaller<T : Any>(
    override val _type: Class<*>,
    val reader: (SerializationCtx, InputStream) -> T,
    val writer: (SerializationCtx, OutputStream, T) -> Unit,
    val predefinedId: Int? = null)
    : IMarshaller<T> {

    override fun read(ctx: SerializationCtx, stream: InputStream) = reader(ctx, stream)
    override fun write(ctx: SerializationCtx, stream: OutputStream, value: T) = writer(ctx, stream, value)
    override val id: RdId
        get() = predefinedId?.let { RdId(IdKind.StaticType, it) } ?: super.id
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
    fun <T : Any> create(type: Class<*>, reader: (InputStream) -> T, writer: (OutputStream, T) -> Unit, predefinedId: Int? = null): UniversalMarshaller<T> {
        return UniversalMarshaller(type, { ctx, stream -> reader(stream) }, { ctx, stream, v -> writer(stream, v) }, predefinedId)
    }
    

    inline fun <reified T : Enum<T>> enum(): UniversalMarshaller<T> {
        return create(T::class.java, { it.readEnum<T>() }, { stream, x -> stream.writeEnum(x) })
    }


    inline fun <TFrom : Any, reified TTo : Any> IMarshaller<TFrom>.delegate(crossinline to: (TFrom) -> TTo, crossinline from: (TTo) -> TFrom): IMarshaller<TTo> {
        return object : IMarshaller<TTo> {
            override val _type: Class<*> get() = TTo::class.java
            override fun read(ctx: SerializationCtx, stream: InputStream): TTo = to(this@delegate.read(ctx, stream))
            override fun write(ctx: SerializationCtx, stream: OutputStream, value: TTo) = this@delegate.write(ctx, stream, from(value))
        }
    }


    val Byte: IMarshaller<Byte> = create(java.lang.Byte::class.java, { it.readByte() }, OutputStream::writeByte, 1)
    val Int16: IMarshaller<Short> = create(java.lang.Short::class.java, { it.readShort() }, OutputStream::writeShort, 2)
    val Int32: IMarshaller<Int> = create(java.lang.Integer::class.java, { it.readInt() }, OutputStream::writeInt, 3)
    val Int64: IMarshaller<Long> = create(java.lang.Long::class.java, { it.readLong() }, OutputStream::writeLong, 4)
    val Float: IMarshaller<Float> = create(java.lang.Float::class.java, { it.readFloat() }, OutputStream::writeFloat, 5)
    val Double: IMarshaller<Double> = create(java.lang.Double::class.java, { it.readDouble() }, OutputStream::writeDouble, 6)
    val Char: IMarshaller<Char> = create(java.lang.Character::class.java, { it.readChar() }, OutputStream::writeChar, 7)
    val Bool: IMarshaller<Boolean> = create(java.lang.Boolean::class.java, { it.readBool() }, OutputStream::writeBool, 8)

    //special
    val Void: IMarshaller<Unit> = create(Unit::class.java, { it.readVoid() }, OutputStream::writeVoid, 9)

    //aliases
    val Short = Int16
    val Int = Int32
    val Long = Int64

    val String: UniversalMarshaller<String> = create(kotlin.String::class.java, { it.readString() }, OutputStream::writeString, 10)
    val Guid: UniversalMarshaller<UUID> = create(UUID::class.java, { it.readUuid() }, OutputStream::writeUuid, 11)
    val DateTime: UniversalMarshaller<Date> = create(java.util.Date::class.java, { it.readDateTime() }, OutputStream::writeDateTime, 12)
    val Uri: UniversalMarshaller<URI> = create(URI::class.java, { it.readUri() }, OutputStream::writeUri, 13)
    val RdId: IMarshaller<RdId> = create(com.jetbrains.rider.framework.RdId::class.java, { it.readRdId() }, OutputStream::writeRdId, 14)

    val ByteArray: UniversalMarshaller<ByteArray> = create(kotlin.ByteArray::class.java, { it.readByteArray() }, OutputStream::writeByteArray, 31)
    val ShortArray: UniversalMarshaller<ShortArray> = create(kotlin.ShortArray::class.java, { it.readShortArray() }, OutputStream::writeShortArray, 32)
    val IntArray: UniversalMarshaller<IntArray> = create(kotlin.IntArray::class.java, { it.readIntArray() }, OutputStream::writeIntArray, 33)
    val LongArray: UniversalMarshaller<LongArray> = create(kotlin.LongArray::class.java, { it.readLongArray() }, OutputStream::writeLongArray, 34)
    val FloatArray: UniversalMarshaller<FloatArray> = create(kotlin.FloatArray::class.java, { it.readFloatArray() }, OutputStream::writeFloatArray, 35)
    val DoubleArray: UniversalMarshaller<DoubleArray> = create(kotlin.DoubleArray::class.java, { it.readDoubleArray() }, OutputStream::writeDoubleArray, 36)
    val CharArray: UniversalMarshaller<CharArray> = create(kotlin.CharArray::class.java, { it.readCharArray() }, OutputStream::writeCharArray, 37)
    val BooleanArray: UniversalMarshaller<BooleanArray> = create(kotlin.BooleanArray::class.java, { it.readBooleanArray() }, OutputStream::writeBooleanArray, 38)


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