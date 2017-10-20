package com.jetbrains.rider.framework

import com.jetbrains.rider.util.PublicApi
import com.jetbrains.rider.util.trace
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.HashSet

@Suppress("UNCHECKED_CAST")
abstract class SerializersBase : ISerializers {

    override val toplevels: MutableSet<Class<*>> = HashSet()

    val types = hashMapOf<RdId,Class<*>>()
    val readers = hashMapOf<RdId, (SerializationCtx, InputStream) -> Any>()
    val writers = hashMapOf<Class<*>, Pair<RdId, (SerializationCtx, OutputStream, Any) -> Any>>()

    init {
        @Suppress("LeakingThis")
        FrameworkMarshallers.registerIn(this)
    }

    override fun <T : Any> register(serializer: IMarshaller<T>) {
        val id = serializer.id
        val t = serializer._type

        Protocol.initializationLogger.trace { "Registering type ${t.simpleName}, id = $id" }

        val existing = types[id]
        if (existing != null ){
            require (existing == t) {"Can't register ${t.name} with id $id, already registered ${existing.name}"}
        } else {
            types[id] = t
        }

        readers.put(id, { ctx, stream -> serializer.read(ctx, stream) })
        writers.put(t, Pair(id, { ctx, stream, v -> serializer.write(ctx, stream, v as T) }))
    }

//    @Suppress("UNUSED_VARIABLE")
//    override fun <T> writeConcrete(clazz: Class<T>, stream: OutputStream, value: T) {
//        val (f, writer) = writers[clazz] ?: throw IllegalStateException("Can't find writer for $clazz")
//        writer(stream, value)
//    }
//
//    override fun <T> writePolymorphic(stream: OutputStream, value: T) {
//        if (value == null) {
//            RdId.Null.write(stream)
//            return;
//        }
//
//        val (id, writer) = writers[value.javaClass] ?: throw IllegalStateException("Can't find writer for ${value.javaClass}")
//        id.write(stream)
//        writer(stream, value)
//    }
//
//    override fun <T : Any> readConcrete(clazz: Class<T>, ctx: SerializationCtx, stream: InputStream): T? {
//        val id = writers[clazz]?.first
//                ?: throw IllegalStateException("Can't find id for class ${clazz.name}")
//
//        return readers[id]?.invoke(ctx, stream) as? T
//                ?: throw IllegalStateException("Can't find reader for $id")
//    }
//
//    override fun <T : Any> readPolymorphic(ctx: SerializationCtx, stream: InputStream): T {
//        val result = readPolymorphicNullable<T>(ctx, stream)
//        return result
//                ?: throw IllegalStateException("Expected value is non-nullable but actual read is null")
//    }
//
//    override fun readPolymorphicNullable<T : Any?>(ctx: SerializationCtx, stream: InputStream): T {
//        val id = RdId.read(stream);
//        if (id.isNull()) return null;
//        return readers[id]?.invoke(ctx, stream) as? T
//                ?: throw IllegalStateException("Can't find reader for $id")
//    }

}

// see http://stackoverflow.com/questions/3706306/c-sharp-datetime-ticks-equivalent-in-java/3706320#3706320
private val TICKS_AT_EPOCH: Long = 621355968000000000L
private val TICKS_PER_MILLISECOND: Long = 10000

// Conversion between .NET Guid (Microsoft GUID Structure) and JVM UUID (RFC 4122)
// See also https://en.wikipedia.org/wiki/Globally_unique_identifier#Binary_encoding
// See also http://referencesource.microsoft.com/#mscorlib/system/guid.cs,58
private fun transformGuidUuid(data : ByteArray) : ByteArray {
    return byteArrayOf(
            data[3], data[2], data[1], data[0],
            data[5], data[4],
            data[7], data[6],
            data[8], data[9], data[10], data[11], data[12], data[13], data[14], data[15])
}

/*reader*/
fun InputStream.readNullableString(): String? {
    val len = readInt()
    if (len < 0) return null

    val res = CharArray(len)
    for (i in res.indices) res[i] = readChar()

    return String(res)
}

//inline fun InputStream.readNullable<reified T : Any>(valueReader : (InputStream) -> T) : T? {
//    return if (readBool()) valueReader(this) else return null
//}

fun InputStream.readString(): String {
    return readNullableString() ?: throw IllegalStateException("String is null")
}

fun InputStream.readUuid(): UUID {
    val bb = ByteBuffer.wrap(transformGuidUuid(readByteArray()))
    return UUID(bb.long, bb.long)
}

fun InputStream.readGuid(): UUID = this.readUuid()

fun InputStream.readDateTime(): Date {
    val l = readLong()
    return Date((l - TICKS_AT_EPOCH) / TICKS_PER_MILLISECOND)
}

fun InputStream.readUri(): URI = URI.create(readString())


private inline fun <T:Any> InputStream.readPrimitivesNullableArray(ctr: (Int) -> T, setter: (T, Int) -> Unit): T? {
    val len = readInt()
    if (len < 0) return null

    val res = ctr(len)
    for (i in 0..len - 1) setter(res, i)
    return res
}
private inline fun <T:Any> InputStream.readPrimitivesArray(ctr: (Int) -> T, setter: (T, Int) -> Unit) = checkNotNull(readPrimitivesNullableArray(ctr, setter))
fun InputStream.readByteArray() = readPrimitivesArray(::ByteArray) { a, i -> a[i] = readByte()}
fun InputStream.readShortArray() = readPrimitivesArray(::ShortArray) { a, i -> a[i] = readShort()}
fun InputStream.readIntArray() = readPrimitivesArray(::IntArray) { a, i -> a[i] = readInt()}
fun InputStream.readLongArray() = readPrimitivesArray(::LongArray) { a, i -> a[i] = readLong()}
fun InputStream.readFloatArray() = readPrimitivesArray(::FloatArray) { a, i -> a[i] = readFloat()}
fun InputStream.readDoubleArray() = readPrimitivesArray(::DoubleArray) { a, i -> a[i] = readDouble()}
fun InputStream.readCharArray() = readPrimitivesArray(::CharArray) { a, i -> a[i] = readChar()}
fun InputStream.readBooleanArray() = readPrimitivesArray(::BooleanArray) { a, i -> a[i] = readBoolean()}


inline fun <T> InputStream.readNullableList(ctx: SerializationCtx, elemReader: (SerializationCtx, InputStream) -> T): List<T>? {
    val len = readInt()
    if (len < 0) return null

    val res = ArrayList<T>(len)
    for (i in 1..len) res.add(elemReader(ctx, this))
    return res
}

inline fun <reified T> InputStream.readList(ctx: SerializationCtx, elemReader: (SerializationCtx, InputStream) -> T): List<T> {
    return readNullableList(ctx, elemReader) ?: throw IllegalStateException("list is null")
}

fun InputStream.readBool(): Boolean {
    return read() != 0
}

fun InputStream.readBoolean() = readBool()

fun InputStream.readByte(): Byte {
    return (read() and 0xff).toByte()
}


fun InputStream.readChar(): Char {
    val c1 = read()
    val c2 = read()
    if ((c1 or c2) < 0) throw EOFException()
    return ((c1 shl 0) + (c2 shl 8)).toChar()
}

fun InputStream.readShort(): Short {
    val c1 = read()
    val c2 = read()
    if ((c1 or c2) < 0) throw EOFException()
    return ((c1 shl 0) + (c2 shl 8)).toShort()
}

fun InputStream.readInt(): Int {
    val c1 = read()
    val c2 = read()
    val c3 = read()
    val c4 = read()
    if ((c1 or c2 or c3 or c4) < 0) throw EOFException()
    return ((c1 shl 0) + (c2 shl 8) + (c3 shl 16) + (c4 shl 24))
}

fun InputStream.readLong(): Long {
    val i1 = readInt()
    val i2 = readInt()
    return (i1.toLong() and 0xffffffff) + (i2.toLong() shl 32)
}

fun InputStream.readFloat(): kotlin.Float {
    val i = readInt()
    return java.lang.Float.intBitsToFloat(i)
}

fun InputStream.readDouble(): kotlin.Double {
    val l = readLong()
    return java.lang.Double.longBitsToDouble(l)
}

@PublicApi
@Suppress("unused")
fun InputStream.readVoid() = Unit

inline fun <reified T : Enum<T>> InputStream.readEnum(): T {
    val intValue = readInt()
    return T::class.java.enumConstants[intValue]
}

fun InputStream.readRdId(): RdId {
    return RdId.read(this)
}


inline fun <T : Any> InputStream.readNullable(inner: () -> T) : T? {
    if (!readBool()) return null
    return inner()
}

@Suppress("unused")
@PublicApi
inline fun <reified T : Any?> InputStream.readArray(inner: () -> T) : Array<T> {
    val len = readInt()
    if (len < 0) throw NullPointerException("Length of array is negative: $len")

    val res = Array(len) { idx -> inner()}
    return res
}

inline fun <reified T : Any?> InputStream.readList(inner: () -> T) : List<T> {
    val len = readInt()
    if (len < 0) throw NullPointerException("Length of array is negative: $len")

    val res = ArrayList<T>(len)
    for (i in 1..len) res.add(inner())
    return res
}


/*writer*/

fun OutputStream.writeChar(x: Char) {
    val xx = x.toInt()
    write((xx) and 0xff)
    write((xx ushr 8) and 0xff)
}

fun OutputStream.writeBool(x: Boolean) = write(if (x) 1 else 0)
fun OutputStream.writeBoolean(x: Boolean) = writeBool(x)

fun OutputStream.writeByte(x: Byte) = write(x.toInt())

fun OutputStream.writeShort(x: Short) {
    write(x.toInt() and 0xff)
    write((x.toInt() ushr  8) and 0xff)
}

fun OutputStream.writeInt(x: Int) {
    write((x) and 0xff)
    write((x ushr  8) and 0xff)
    write((x ushr 16) and 0xff)
    write((x ushr 24) and 0xff)
}

fun Int.writeIntoByteArray(dst: ByteArray, offset : Int = 0) {
    dst[offset] = (this and 0xff).toByte()
    dst[offset + 1] = ((this ushr  8) and 0xff).toByte()
    dst[offset + 2] = ((this ushr  16) and 0xff).toByte()
    dst[offset + 3] = ((this ushr  24) and 0xff).toByte()

}

fun OutputStream.writeLong(x: Long) {
    writeInt(((x) and 0xffffffff).toInt())
    writeInt(((x ushr 32) and 0xffffffff).toInt())
}

fun OutputStream.writeDouble(x: kotlin.Double) {
    writeLong(java.lang.Double.doubleToRawLongBits(x))
}

fun OutputStream.writeFloat(x: kotlin.Float) {
    writeInt(java.lang.Float.floatToRawIntBits(x))
}

@PublicApi
@Suppress("unused")
fun OutputStream.writeVoid(@Suppress("UNUSED_PARAMETER") tmp: Unit) {}

fun OutputStream.writeNullableString(value: String?) {
    value ?: let {
        writeInt(-1)
        return
    }

    writeInt(value.length)
    for (c in value) writeChar(c)
}

fun OutputStream.writeString(value: String) {
    writeNullableString(value)
}

private inline fun <T:Any> OutputStream.writePrimitivesNullableArray(array: T?, getLength: (T) -> Int, writeElem: (Int) -> Unit) {
    if (array == null) {
        writeInt(-1)
        return
    }
    
    val len = getLength(array)
    writeInt(len)

    for (i in 0..len - 1) writeElem(i)
}
private inline fun <T:Any> OutputStream.writePrimitivesArray(array: T?, getLength: (T) -> Int, writeElem: (Int) -> Unit) = writePrimitivesNullableArray(array, getLength, writeElem)
fun OutputStream.writeByteArray(value: ByteArray) = writePrimitivesArray(value, {it.size}) { i -> writeByte(value[i])}
fun OutputStream.writeShortArray(value: ShortArray) = writePrimitivesArray(value, {it.size}) { i ->writeShort(value[i])}
fun OutputStream.writeIntArray(value: IntArray) = writePrimitivesArray(value, {it.size}) { i ->writeInt(value[i])}
fun OutputStream.writeLongArray(value: LongArray) = writePrimitivesArray(value, {it.size}) { i ->writeLong(value[i])}
fun OutputStream.writeFloatArray(value: FloatArray) = writePrimitivesArray(value, {it.size}) { i ->writeFloat(value[i])}
fun OutputStream.writeDoubleArray(value: DoubleArray) = writePrimitivesArray(value, {it.size}) { i ->writeDouble(value[i])}
fun OutputStream.writeCharArray(value: CharArray) = writePrimitivesArray(value, {it.size}) { i ->writeChar(value[i])}
fun OutputStream.writeBooleanArray(value: BooleanArray) = writePrimitivesArray(value, {it.size}) { i -> writeBoolean(value[i])}



fun OutputStream.writeUuid(value: UUID) {
    writeByteArray(transformGuidUuid(ByteBuffer.allocate(16).putLong(value.mostSignificantBits).putLong(value.leastSignificantBits).array()))
}

fun OutputStream.writeGuid(value: UUID) = writeUuid(value)

fun OutputStream.writeDateTime(value: Date) {
    writeLong(value.time * TICKS_PER_MILLISECOND + TICKS_AT_EPOCH)
}

fun OutputStream.writeUri(value: URI) {
    writeString(value.toString())
}

fun <T: Any> OutputStream.writeNullable(value : T?, elemWriter:(T) -> Unit) {
    if (value == null) writeBool(false)
    else {
        writeBool(true)
        elemWriter(value)
    }
}

@PublicApi
@Suppress("unused")
fun <T: Any?> OutputStream.writeArray(value : Array<T>, elemWriter:(T) -> Unit) {
    writeInt(value.size)
    value.forEach { elemWriter(it) }
}

fun <T: Any?> OutputStream.writeList(value : List<T>, elemWriter:(T) -> Unit) {
    writeInt(value.size)
    value.forEach { elemWriter(it) }
}

inline fun <reified T : Enum<T>> OutputStream.writeEnum(value: Enum<T>) {
    writeInt(value.ordinal)
}

inline fun <reified T> OutputStream.writeNullableList(value: List<T>?, elemWriter: (OutputStream, T) -> Unit) {
    value ?: let {
        writeInt(-1)
        return
    }

    writeInt(value.size)
    for (item in value) elemWriter(this, item)
}

inline fun <reified T> OutputStream.writeList(value: List<T>, elemWriter: (OutputStream, T) -> Unit) {
    writeNullableList(value, elemWriter)
}

fun OutputStream.writeRdId(value: RdId) {
    value.write(this)
}
