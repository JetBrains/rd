package com.jetbrains.rider.rdtext.impl.intrinsics

import com.jetbrains.rider.framework.*
import com.jetbrains.rider.rdtext.RdTextChange
import com.jetbrains.rider.rdtext.RdTextChangeKind
import kotlin.reflect.KClass

@Suppress("unused")
object RdTextChangeMarshaller: IMarshaller<RdTextChange> {

        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): RdTextChange {
            val kind = buffer.readEnum<RdTextChangeKind>()
            val offset = buffer.readInt()
            val old = buffer.readString()
            val new = buffer.readString()
            val fullLength = buffer.readInt()
            return RdTextChange(kind, offset, old, new, fullLength)
        }

        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: RdTextChange) {
            buffer.writeEnum(value.kind)
            buffer.writeInt(value.startOffset)
            buffer.writeString(value.old)
            buffer.writeString(value.new)
            buffer.writeInt(value.fullTextLength)
        }

        override val _type: KClass<RdTextChange> get() = RdTextChange::class
}