package com.jetbrains.rider.rdtext.impl.ot.intrinsics

import com.jetbrains.rider.framework.*
import com.jetbrains.rider.rdtext.RdChangeOrigin
import com.jetbrains.rider.rdtext.impl.ot.RdAck
import kotlin.reflect.KClass

@Suppress("unused")
object RdAckMarshaller : IMarshaller<RdAck> {
    override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): RdAck {
        val ts = buffer.readInt()
        val origin = buffer.readEnum<RdChangeOrigin>()
        return RdAck(ts, origin)
    }

    override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: RdAck) {
        buffer.writeInt(value.timestamp)
        buffer.writeEnum(value.origin)
    }

    override val _type: KClass<*>
        get() = RdAck::class

}