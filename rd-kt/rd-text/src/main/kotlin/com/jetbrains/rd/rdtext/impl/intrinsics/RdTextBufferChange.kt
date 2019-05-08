package com.jetbrains.rd.rdtext.impl.intrinsics

import com.jetbrains.rd.framework.IMarshaller
import com.jetbrains.rd.framework.SerializationCtx
import com.jetbrains.rd.framework.AbstractBuffer
import com.jetbrains.rd.rdtext.intrinsics.TextBufferVersion
import com.jetbrains.rd.rdtext.intrinsics.RdTextChange
import com.jetbrains.rd.framework.readEnum
import com.jetbrains.rd.framework.writeEnum
import com.jetbrains.rd.rdtext.intrinsics.RdTextChangeMarshaller
import com.jetbrains.rd.util.string.IPrintable
import com.jetbrains.rd.util.string.PrettyPrinter
import com.jetbrains.rd.util.string.printToString
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
