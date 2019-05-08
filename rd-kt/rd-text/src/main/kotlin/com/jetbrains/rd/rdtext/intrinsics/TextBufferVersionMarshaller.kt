package com.jetbrains.rd.rdtext.intrinsics

import com.jetbrains.rd.framework.*
import kotlin.reflect.KClass

@Suppress("unused")
object TextBufferVersionMarshaller: IMarshaller<TextBufferVersion> {

    override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): TextBufferVersion {
        val masterVersion = buffer.readInt()
        val slaveVersion = buffer.readInt()
        return TextBufferVersion(masterVersion, slaveVersion)
    }

    override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: TextBufferVersion) {
        buffer.writeInt(value.master)
        buffer.writeInt(value.slave)
    }

    override val _type: KClass<TextBufferVersion> get() = TextBufferVersion::class
}