package com.jetbrains.rider.framework

import com.jetbrains.rider.framework.base.AbstractBuffer
import com.jetbrains.rider.framework.base.ISerializersOwner
import com.jetbrains.rider.framework.impl.RdSecureString
import com.jetbrains.rider.util.PublicApi
import com.jetbrains.rider.util.trace
import java.net.URI
import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.HashSet

private const val notRegisteredErrorMessage = "Maybe you forgot to invoke 'register()' method of corresponding Toplevel. " +
    "Usually it should be done automagically during 'bind()' invocation but in complex cases you should do it manually."

@Suppress("UNCHECKED_CAST")
class Serializers : ISerializers {

    override val toplevels: MutableSet<Class<ISerializersOwner>> = HashSet()

    val types = hashMapOf<RdId,Class<*>>()
    val readers = hashMapOf<RdId, (SerializationCtx, AbstractBuffer) -> Any>()
    val writers = hashMapOf<Class<*>, Pair<RdId, (SerializationCtx, AbstractBuffer, Any) -> Unit>>()

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
            require (existing == t) { "Can't register ${t.name} with id: $id, already registered: ${existing.name}" }
        } else {
            types[id] = t
        }

        readers.put(id, serializer::read)
        writers.put(t, Pair(id, serializer::write) as Pair<RdId, (SerializationCtx, AbstractBuffer, Any) -> Unit>)
    }

    override fun <T> readPolymorphicNullable(ctx: SerializationCtx, stream: AbstractBuffer): T? {
        val id = RdId.read(stream)
        if (id.isNull) return null

        val reader = readers[id] ?: throw IllegalStateException("Can't find reader by id: $id. $notRegisteredErrorMessage")

        return reader.invoke(ctx, stream) as T
    }

    override fun <T> writePolymorphicNullable(ctx: SerializationCtx, stream: AbstractBuffer, value: T) {
        if (value == null) {
            RdId.Null.write(stream)
            return
        }
        writePolymorphic(ctx, stream, value as Any)
    }

    override fun <T : Any> readPolymorphic(ctx: SerializationCtx, stream: AbstractBuffer): T {
        return readPolymorphicNullable(ctx, stream) ?: throw IllegalStateException("Non-null object expected")
    }

    override fun <T : Any> writePolymorphic(ctx: SerializationCtx, stream: AbstractBuffer, value: T) {
        val (id, writer) = writers[value::class.java] ?: throw IllegalStateException("Can't find writer by class: ${value::class}. $notRegisteredErrorMessage")
        id.write(stream)
        writer(ctx, stream, value)
    }
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

//inline fun InputStream.readNullable<reified T : Any>(valueReader : (InputStream) -> T) : T? {
//    return if (readBool()) valueReader(this) else return null
//}

fun AbstractBuffer.readUuid(): UUID {
    val bb = ByteBuffer.wrap(transformGuidUuid(readByteArray()))
    return UUID(bb.long, bb.long)
}

fun AbstractBuffer.readGuid(): UUID = this.readUuid()

fun AbstractBuffer.readDateTime(): Date {
    val l = readLong()
    return Date((l - TICKS_AT_EPOCH) / TICKS_PER_MILLISECOND)
}

fun AbstractBuffer.readUri(): URI = URI.create(readString())

inline fun <reified T : Enum<T>> AbstractBuffer.readEnum(): T {
    val intValue = readInt()
    return T::class.java.enumConstants[intValue]
}

fun AbstractBuffer.readRdId(): RdId {
    return RdId.read(this)
}

inline fun <T : Any> AbstractBuffer.readNullable(inner: () -> T) : T? {
    if (!readBoolean()) return null
    return inner()
}

@Suppress("unused")
@PublicApi
inline fun <reified T : Any?> AbstractBuffer.readArray(inner: () -> T) : Array<T> {
    val len = readInt()
    if (len < 0) throw NullPointerException("Length of array is negative: $len")

    return Array(len) { _ -> inner() }
}

inline fun <reified T> AbstractBuffer.readList(inner: () -> T): List<T> {
    val len = readInt()
    if (len < 0) throw NullPointerException("Length of array is negative: $len")

    val res = ArrayList<T>(len)
    for (i in 1..len) res.add(inner())
    return res
}

/*writer*/

fun AbstractBuffer.writeUuid(value: UUID) {
    writeByteArray(transformGuidUuid(ByteBuffer.allocate(16).putLong(value.mostSignificantBits).putLong(value.leastSignificantBits).array()))
}

fun AbstractBuffer.writeGuid(value: UUID) = writeUuid(value)

fun AbstractBuffer.writeDateTime(value: Date) {
    writeLong(value.time * TICKS_PER_MILLISECOND + TICKS_AT_EPOCH)
}

fun AbstractBuffer.writeUri(value: URI) {
    writeString(value.toString())
}

fun <T: Any> AbstractBuffer.writeNullable(value : T?, elemWriter:(T) -> Unit) {
    if (value == null) writeBoolean(false)
    else {
        writeBoolean(true)
        elemWriter(value)
    }
}

@PublicApi
@Suppress("unused")
fun <T: Any?> AbstractBuffer.writeArray(value : Array<T>, elemWriter:(T) -> Unit) {
    writeInt(value.size)
    value.forEach { elemWriter(it) }
}

inline fun <reified T> AbstractBuffer.writeList(value: List<T>, elemWriter: (T) -> Unit) {
    writeInt(value.size)
    value.forEach { elemWriter(it) }
}

inline fun <reified T : Enum<T>> AbstractBuffer.writeEnum(value: Enum<T>) {
    writeInt(value.ordinal)
}

fun AbstractBuffer.writeRdId(value: RdId) {
    value.write(this)
}

inline fun AbstractBuffer.writeBool(value: Boolean) = writeBoolean(value)
inline fun AbstractBuffer.readBool() = readBoolean()

fun AbstractBuffer.readVoid() = Unit
fun AbstractBuffer.writeVoid(void: Unit) = Unit

fun AbstractBuffer.readSecureString() = RdSecureString(readString())
fun AbstractBuffer.writeSecureString(string: RdSecureString) = writeString(string.contents)