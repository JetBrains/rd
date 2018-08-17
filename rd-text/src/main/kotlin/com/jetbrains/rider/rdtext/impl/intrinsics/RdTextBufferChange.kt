package com.jetbrains.rider.rdtext.impl.intrinsics

import com.jetbrains.rider.framework.IMarshaller
import com.jetbrains.rider.framework.SerializationCtx
import com.jetbrains.rider.framework.AbstractBuffer
import com.jetbrains.rider.rdtext.TextBufferVersion
import com.jetbrains.rider.rdtext.intrinsics.RdTextChange
import com.jetbrains.rider.framework.readEnum
import com.jetbrains.rider.framework.writeEnum
import com.jetbrains.rider.rdtext.intrinsics.RdTextChangeMarshaller
import com.jetbrains.rider.util.string.IPrintable
import com.jetbrains.rider.util.string.PrettyPrinter
import com.jetbrains.rider.util.string.printToString
import kotlin.reflect.*

data class RdTextBufferChange(val version: TextBufferVersion, val origin: RdChangeOrigin, val change: RdTextChange): IPrintable {
    companion object : IMarshaller<RdTextBufferChange> {

        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): RdTextBufferChange {
            val masterVersionRemote = buffer.readInt()
            val slaveVersionRemote = buffer.readInt()
            val version = TextBufferVersion(masterVersionRemote, slaveVersionRemote)
            val origin = buffer.readEnum<RdChangeOrigin>()
            val change = RdTextChangeMarshaller.read(ctx, buffer)
            return RdTextBufferChange(version, origin, change)
        }

        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: RdTextBufferChange) {
            val version = value.version
            buffer.writeInt(version.master)
            buffer.writeInt(version.slave)
            val origin = value.origin
            buffer.writeEnum(origin)
            val change = value.change
            RdTextChangeMarshaller.write(ctx, buffer, change)
        }

        override val _type: KClass<RdTextBufferChange> get() = RdTextBufferChange::class
    }

    override fun print(printer: PrettyPrinter) {
        printer.println("RdTextBufferChange (")
        printer.indent {
            println("version = (master=${version.master}, slave=${version.slave})")
            println("side = $origin")
            print("change = ")
            change.print(printer)
        }
        printer.println(")")
    }

    override fun toString(): String = this.printToString()
}
