package com.jetbrains.rd.framework

import com.jetbrains.rd.framework.base.ISerializersOwner
import com.jetbrains.rd.framework.impl.RdSecureString
import com.jetbrains.rd.util.*
import com.jetbrains.rd.util.lifetime.Lifetime
import kotlin.reflect.KClass

private const val notRegisteredErrorMessage = "Maybe you forgot to invoke 'register()' method of corresponding Toplevel. " +
        "Usually it should be done automatically during 'bind()' invocation but in complex cases you should do it manually."


@Suppress("UNCHECKED_CAST")
class Serializers : ISerializers {

    companion object {
        private val backgroundRegistrar =  createBackgroundScheduler(Lifetime.Eternal, "SerializersBackgroundRegistrar")
    }
    override fun registerSerializersOwnerOnce(serializersOwner: ISerializersOwner) {
        backgroundRegistrar.invokeOrQueue {
            val key = serializersOwner::class
            if (toplevels.add(key)) {
                Protocol.initializationLogger.trace { "REGISTER serializers for ${key.simpleName}" }
                serializersOwner.registerSerializersCore(this)
            }
        }
    }

    private val toplevels: MutableSet<KClass<out ISerializersOwner>> = HashSet()



    val types = hashMapOf<RdId, KClass<*>>()
    val writers = hashMapOf<KClass<*>, Pair<RdId, (SerializationCtx, AbstractBuffer, Any) -> Unit>>()
    val marshallers = hashMapOf<RdId, IMarshaller<*>>()

    init {
        backgroundRegistrar.invokeOrQueue {
            FrameworkMarshallers.registerIn(this)
        }
    }

    override fun <T : Any> register(serializer: IMarshaller<T>) {
        if (!backgroundRegistrar.isActive) {
            backgroundRegistrar.queue { register(serializer) }
            return
        }

        val id = serializer.id
        val t = serializer._type

        Protocol.initializationLogger.trace { "Registering type ${t.simpleName}, id = $id" }

        val existing = types[id]
        if (existing != null) {
            require(existing == t) { "Can't register ${t.simpleName} with id: $id, already registered: ${existing.simpleName}" }
        } else {
            types[id] = t
        }

        marshallers[id] = serializer
        writers[t] = Pair(id, serializer::write) as Pair<RdId, (SerializationCtx, AbstractBuffer, Any) -> Unit>
    }

    override fun get(id: RdId): IMarshaller<*>? {
        return marshallers[id]
    }

    override fun <T> readPolymorphicNullable(ctx: SerializationCtx, stream: AbstractBuffer, abstractDeclaration: IAbstractDeclaration<T>?): T? {
        backgroundRegistrar.flush()

        val id = RdId.read(stream)
        if (id.isNull) return null
        val size = stream.readInt()
        stream.checkAvailable(size)

        val reader = marshallers[id]
        if (reader == null) {
            if (abstractDeclaration == null) {
                throw IllegalStateException("Can't find reader by id: $id. $notRegisteredErrorMessage")
            }

            return abstractDeclaration.readUnknownInstance(ctx, stream, id, size)
        }

        return reader.read(ctx, stream) as T
    }

    override fun <T> writePolymorphicNullable(ctx: SerializationCtx, stream: AbstractBuffer, value: T) {
        if (value == null) {
            RdId.Null.write(stream)
            return
        }
        writePolymorphic(ctx, stream, value as Any)
    }

    override fun <T : Any> readPolymorphic(ctx: SerializationCtx, stream: AbstractBuffer, abstractDeclaration: IAbstractDeclaration<T>?): T {
        return readPolymorphicNullable(ctx, stream, abstractDeclaration)
                ?: throw IllegalStateException("Non-null object expected")
    }

    override fun <T : Any> writePolymorphic(ctx: SerializationCtx, stream: AbstractBuffer, value: T) {
        backgroundRegistrar.flush()

        val (id, writer) = writers[value::class]
                ?: throw IllegalStateException("Can't find writer by class: ${value::class}. $notRegisteredErrorMessage")

        if (value is IUnknownInstance) {
            value.unknownId.write(stream)
        } else {
            id.write(stream)
        }

        val lengthTagPosition = stream.position
        stream.writeInt(0)
        val objectStartPosition = stream.position
        writer(ctx, stream, value)
        val objectEndPosition = stream.position
        stream.position = lengthTagPosition
        stream.writeInt(objectEndPosition - objectStartPosition)
        stream.position = objectEndPosition
    }
}

// see http://stackoverflow.com/questions/3706306/c-sharp-datetime-ticks-equivalent-in-java/3706320#3706320
private val TICKS_AT_EPOCH: Long = 621355968000000000L
private val TICKS_PER_MILLISECOND: Long = 10000

// Conversion between .NET Guid (Microsoft GUID Structure) and JVM UUID (RFC 4122)
// See also https://en.wikipedia.org/wiki/Globally_unique_identifier#Binary_encoding
// See also http://referencesource.microsoft.com/#mscorlib/system/guid.cs,58
private fun transformGuidUuid(data: ByteArray): ByteArray {
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
//    val bb = ByteBuffer.wrap(transformGuidUuid(readByteArray()))
    val data = transformGuidUuid(readByteArray())
    require(data.size == 16)

    return UUID(data.parseLong(0), data.parseLong(8))
}

fun AbstractBuffer.readGuid(): UUID = this.readUuid()

fun AbstractBuffer.readDateTime(): Date {
    val timeInTicks = readLong()
    val timeInMillisecondsSinceEpoch = (timeInTicks - TICKS_AT_EPOCH) / TICKS_PER_MILLISECOND
    return Date(timeInMillisecondsSinceEpoch)
}

fun AbstractBuffer.readUri(): URI = URI(readString())

inline fun <reified T : Enum<T>> AbstractBuffer.readEnum(): T {
    val ordinal = readInt()
    return parseFromOrdinal(ordinal)
}

inline fun <reified T : Enum<T>> AbstractBuffer.readEnumSet(): EnumSet<T> {
    val flags = readInt()
    return parseFromFlags(flags)
}

fun AbstractBuffer.readRdId(): RdId {
    return RdId.read(this)
}

inline fun <T : Any> AbstractBuffer.readNullable(inner: () -> T): T? {
    if (!readBoolean()) return null
    return inner()
}

@Suppress("unused")
@PublicApi
inline fun <reified T : Any?> AbstractBuffer.readArray(inner: () -> T): Array<T> {
    val len = readInt()
    if (len < 0) throw NullPointerException("Length of array is negative: $len")

    return Array(len) { inner() }
}

inline fun <T> AbstractBuffer.readList(inner: () -> T): List<T> {
    val len = readInt()
    if (len < 0) throw NullPointerException("Length of array is negative: $len")

    val res = ArrayList<T>(len)
    for (i in 1..len) res.add(inner())
    return res
}

/*writer*/

fun AbstractBuffer.writeUuid(value: UUID) {
    val uuidBytes = ByteArray(16)
    uuidBytes.putLong(value.getMostSignificantBits(), 0)
    uuidBytes.putLong(value.getLeastSignificantBits(), 8)

    val guidBytes = transformGuidUuid(uuidBytes)
    writeByteArray(guidBytes)
}

fun AbstractBuffer.writeGuid(value: UUID) = writeUuid(value)

fun AbstractBuffer.writeDateTime(value: Date) {
    val timeInTicks = value.getTime() * TICKS_PER_MILLISECOND + TICKS_AT_EPOCH
    writeLong(timeInTicks)
}

fun AbstractBuffer.writeUri(value: URI) {
    writeString(value.toString())
}

fun <T : Any> AbstractBuffer.writeNullable(value: T?, elemWriter: (T) -> Unit) {
    if (value == null) writeBoolean(false)
    else {
        writeBoolean(true)
        elemWriter(value)
    }
}

@PublicApi
@Suppress("unused")
fun <T : Any?> AbstractBuffer.writeArray(value: Array<T>, elemWriter: (T) -> Unit) {
    writeInt(value.size)
    value.forEach { elemWriter(it) }
}

inline fun <T> AbstractBuffer.writeList(value: List<T>, elemWriter: (T) -> Unit) {
    writeInt(value.size)
    value.forEach { elemWriter(it) }
}

inline fun <reified T : Enum<T>> AbstractBuffer.writeEnum(value: Enum<T>) {
    writeInt(value.ordinal)
}

inline fun <reified T : Enum<T>> AbstractBuffer.writeEnumSet(set: EnumSet<T>) {
    val flags = set.values().fold(0) { acc, nxt -> acc or (1 shl nxt.ordinal) }
    writeInt(flags)
}

fun AbstractBuffer.writeRdId(value: RdId) {
    value.write(this)
}

fun AbstractBuffer.writeBool(value: Boolean) = writeBoolean(value)
fun AbstractBuffer.readBool() = readBoolean()

fun AbstractBuffer.readVoid() = Unit
@Suppress("UNUSED_PARAMETER")
fun AbstractBuffer.writeVoid(void: Unit) = Unit

fun AbstractBuffer.readSecureString() = RdSecureString(readString())
fun AbstractBuffer.writeSecureString(string: RdSecureString) = writeString(string.contents)