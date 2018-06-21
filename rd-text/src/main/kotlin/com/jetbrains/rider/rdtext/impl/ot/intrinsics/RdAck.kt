package com.jetbrains.rider.rdtext.impl.ot.intrinsics

import com.jetbrains.rider.framework.*
import com.jetbrains.rider.util.string.IPrintable
import com.jetbrains.rider.util.string.PrettyPrinter
import com.jetbrains.rider.util.string.print
import kotlin.reflect.KClass

data class RdAck (
        val timestamp : Int,
        val origin : com.jetbrains.rider.rdtext.impl.intrinsics.RdChangeOrigin
) : IPrintable {
    //companion

    companion object : IMarshaller<RdAck> {
        override val _type: KClass<RdAck> = RdAck::class

        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): RdAck {
            val timestamp = buffer.readInt()
            val origin = buffer.readEnum<com.jetbrains.rider.rdtext.impl.intrinsics.RdChangeOrigin>()
            return RdAck(timestamp, origin)
        }

        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: RdAck) {
            buffer.writeInt(value.timestamp)
            buffer.writeEnum(value.origin)
        }

    }
    //fields
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as RdAck

        if (timestamp != other.timestamp) return false
        if (origin != other.origin) return false

        return true
    }
    //hash code trait
    override fun hashCode(): Int {
        var __r = 0
        __r = __r*31 + timestamp.hashCode()
        __r = __r*31 + origin.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter) {
        printer.println("RdAck (")
        printer.indent {
            print("timestamp = "); timestamp.print(printer); println()
            print("origin = "); origin.print(printer); println()
        }
        printer.print(")")
    }
}