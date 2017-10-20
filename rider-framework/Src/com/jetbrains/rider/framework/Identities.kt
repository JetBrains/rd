//package com.jetbrains.rider.framework
//
//
//import java.io.InputStream
//import java.io.OutputStream
//import java.nio.charset.Charset
//import java.util.concurrent.atomic.AtomicInteger
//
//
//enum class IdKind(val value : Int) {
//
//    StaticType(0),
//    StaticEntity(1),
//    DynamicClient(2),
//    DynamicServer(3);
//
//    init {
//        assert(value in 0..3) { "Must take exactly two bits: $value" }
//    }
//}
//
//
//class RdHash(val value : Long) {
//
//    //todo use murmurhash3
//    class Hasher {
//        private var value: Long = 19
//
//        private fun putByte(v : Byte) = apply { value = value * 31 + java.lang.Byte.toUnsignedInt(v)}
//        fun putBytes(v : ByteArray) = apply { v.forEach { putByte(it) } }
//        fun putString(v : String)   = putBytes(v.toByteArray())
//
//        fun hash() : RdHash = RdHash(value)
//    }
//
//    companion object {
//        fun createHasher() = Hasher()
//
//
//    }
//    override fun toString(): String = value.toString()
//}
//
//
//data class RdId constructor(val kind : IdKind, val hash: Int) {
//
//    companion object {
//        val Null : RdId = RdId(IdKind.StaticType, 0)
//
//        fun read(stream: InputStream) : RdId {
//            val i = stream.readInt()
//            val kind = IdKind.values()[i]
//            val number = stream.readInt()
//            return RdId(kind, number)
//        }
//    }
//
//    fun write(stream: OutputStream) {
//        stream.writeInt(kind.value)
//        stream.writeInt(hash)
//    }
//
//    val isNull: Boolean get() { return equals(Null) }
//
//    override fun toString(): String {
//        return "${kind.name}/$hash"
//    }
//
//    fun notNull() : RdId {
//        require(!isNull) {"id is null"}
//        return this
//    }
//}
//
//class Identities private constructor() : IIdentities {
//    companion object {
//
//    }
//
//    fun sub() : Identities {
//
//    }
//
//    val id : RdId =
////    private val idAcc  = AtomicInteger(initialNumber)
////
////    override fun next() : RdId {
////        @Suppress("UsePropertyAccessSyntax")
////        return RdId(dynamicKind, idAcc.getAndIncrement())
////    }
//
//}

package com.jetbrains.rider.framework


import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicInteger

public enum class IdKind(val value : Int) {

    StaticType(0),
    StaticEntity(1),
    DynamicClient(2),
    DynamicServer(3);

    init {
        assert(value >= 0 && value <= 3) { "Must take exactly two bits: $value" };
    }
}

public data class RdId(public val kind : IdKind, public val hash : Int) {

    companion object {
        public val Null : RdId = RdId(IdKind.StaticType, 0)

        fun read(stream: InputStream) : RdId {
            val i = stream.readInt()
            val kind = IdKind.values()[i]
            val number = stream.readInt()
            return RdId(kind, number)
        }
    }

    init {
        //assert(number >= 0 && number <= ((1 shl 30) - 1), "Invalid number: $number")
    }

    fun write(stream: OutputStream) {
        stream.writeInt(kind.value)
        stream.writeInt(hash)
    }

    val isStaticEntity: Boolean get() = kind == IdKind.StaticEntity
    val isNull: Boolean get() { return equals(Null) }

    override fun toString(): String {
        return "${kind.name}/$hash"
    }

    fun notNull() : RdId {
        require(!isNull) {"id is null"}
        return this
    }
}

public class Identities(val dynamicKind : IdKind = IdKind.DynamicClient) : IIdentities {
    private val idAcc = AtomicInteger(1);

    public override fun next(): RdId {
        return RdId(dynamicKind, idAcc.getAndIncrement());
    }
}
