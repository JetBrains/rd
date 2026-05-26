package com.jetbrains.rd.framework


import com.jetbrains.rd.util.AtomicInteger
import com.jetbrains.rd.util.AtomicLong
import com.jetbrains.rd.util.hash.getPlatformIndependentHash
import com.jetbrains.rd.util.string.condstr

enum class IdKind {
    Client,
    Server
}

//PLEASE DO NOT CHANGE IT!!! IT'S EXACTLY THE SAME ON C# SIDE
@Deprecated("Api moved to com.jetbrains.rd.util.hash", ReplaceWith("getPlatformIndependentHash(initial)","com.jetbrains.rd.util.hash.getPlatformIndependentHash"))
fun String?.getPlatformIndependentHash(initial: Long = 19L) : Long = getPlatformIndependentHash(initial)
@Deprecated("Api moved to com.jetbrains.rd.util.hash", ReplaceWith("getPlatformIndependentHash(initial)","com.jetbrains.rd.util.hash.getPlatformIndependentHash"))
fun Int.getPlatformIndependentHash(initial: Long = 19L) : Long = getPlatformIndependentHash(initial)
@Deprecated("Api moved to com.jetbrains.rd.util.hash", ReplaceWith("getPlatformIndependentHash(initial)","com.jetbrains.rd.util.hash.getPlatformIndependentHash"))
fun Long.getPlatformIndependentHash(initial: Long = 19L) : Long = getPlatformIndependentHash(initial)


/**
 * Utility object for RdId hash computation operations.
 * This is internal - for ID generation, use [IIdentities.mix] and [IIdentities.next].
 */
object RdIdUtil {
    internal fun mix(rdId: RdId, tail: String): RdId {
        return RdId(tail.getPlatformIndependentHash(rdId.hash))
    }

    internal fun mix(rdId: RdId, tail: Int): RdId {
        return RdId(tail.getPlatformIndependentHash(rdId.hash))
    }

    internal fun mix(rdId: RdId, tail: Long): RdId {
        return RdId(tail.getPlatformIndependentHash(rdId.hash))
    }
}


/**
 * An identifier of the object that participates in the object graph.
 */
@JvmInline
value class RdId(val hash: Long) {

    companion object {
        val Null : RdId = RdId( 0)
        const val MAX_STATIC_ID = 1_000_000

        fun read(buffer: AbstractBuffer) : RdId {
            val number = buffer.readLong()
            return RdId(number)
        }

        override fun toString(): String {
            throw UnsupportedOperationException("Don't tostring RdId companion")
        }
    }


    fun write(buffer: AbstractBuffer) {
        buffer.writeLong(hash)
    }

    val isNull: Boolean get() { return equals(Null) }

    override fun toString(): String {
        // java.lang.Long.toUnsignedString(hash)
        val quot = hash.ushr(1) / 5
        val rem = hash - quot * 10
        return (quot > 0).condstr { quot.toString() } + rem
    }

    fun notNull() : RdId {
        require(!isNull) {"id is null"}
        return this
    }
}

/**
 * Legacy implementation of [IIdentities] that mixes parent ID into dynamic IDs.
 * This approach can cause ID collisions. Use [SequentialIdentities] instead.
 */
@Deprecated("Use SequentialIdentities instead", ReplaceWith("SequentialIdentities(dynamicKind)"))
class Identities(override val dynamicKind : IdKind) : IIdentities {
    private var idAcc = AtomicInteger(when(dynamicKind) {
        IdKind.Client -> BASE_CLIENT_ID
        IdKind.Server -> BASE_SERVER_ID
    })

    companion object {
        private const val BASE_CLIENT_ID = RdId.MAX_STATIC_ID
        private const val BASE_SERVER_ID = RdId.MAX_STATIC_ID + 1
    }

    override fun next(parent: RdId): RdId {
        return RdIdUtil.mix(parent, idAcc.getAndAdd(2))
    }

    override fun mix(rdId: RdId, tail: String): RdId {
        return RdIdUtil.mix(rdId, tail)
    }

    override fun mix(rdId: RdId, tail: Int): RdId {
        return RdIdUtil.mix(rdId, tail)
    }

    override fun mix(rdId: RdId, tail: Long): RdId {
        return RdIdUtil.mix(rdId, tail)
    }
}

/**
 * Recommended implementation of [IIdentities] that avoids ID collisions.
 *
 * - Dynamic IDs ([next]): Sequential integers that ignore the parent ID.
 *   Client IDs are even, server IDs are odd, ensuring no overlap.
 * - Stable IDs ([mix]): Hash-based with the high bit set (0x8000000000000000) to ensure
 *   they never collide with dynamic IDs. The number of stable entities is small,
 *   so hash collisions are unlikely.
 */
class SequentialIdentities(override val dynamicKind : IdKind) : IIdentities {
    private var idAcc = AtomicLong(when(dynamicKind) {
        IdKind.Client -> BASE_CLIENT_ID
        IdKind.Server -> BASE_SERVER_ID
    })

    companion object {
        private const val BASE_CLIENT_ID = RdId.MAX_STATIC_ID.toLong()
        private const val BASE_SERVER_ID = RdId.MAX_STATIC_ID.toLong() + 1

        /** High bit mask to distinguish stable IDs from dynamic IDs */
        private const val StableMask = 1L shl 63
    }

    override fun next(parent: RdId): RdId {
        // Ignore parent to avoid collisions from different creation order on client/server
        return RdId(idAcc.getAndAdd(2))
    }

    override fun mix(rdId: RdId, tail: String): RdId {
        return RdId(StableMask or RdIdUtil.mix(rdId, tail).hash)
    }

    override fun mix(rdId: RdId, tail: Int): RdId {
        return RdId(StableMask or RdIdUtil.mix(rdId, tail).hash)
    }

    override fun mix(rdId: RdId, tail: Long): RdId {
        return RdId(StableMask or RdIdUtil.mix(rdId, tail).hash)
    }
}
