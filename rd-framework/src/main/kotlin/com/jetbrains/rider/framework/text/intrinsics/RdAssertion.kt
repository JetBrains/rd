package com.jetbrains.rider.framework.text.intrinsics

import com.jetbrains.rider.framework.IMarshaller
import com.jetbrains.rider.framework.SerializationCtx
import com.jetbrains.rider.framework.AbstractBuffer
import com.jetbrains.rider.util.string.IPrintable
import com.jetbrains.rider.util.string.PrettyPrinter
import com.jetbrains.rider.util.string.print
import kotlin.reflect.*

data class RdAssertion (
        val masterVersion : Int,
        val slaveVersion : Int,
        val text : String
) : IPrintable {
    //companion

    companion object : IMarshaller<RdAssertion> {
        override val _type: KClass<RdAssertion> = RdAssertion::class

        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): RdAssertion {
            val masterVersion = buffer.readInt()
            val slaveVersion = buffer.readInt()
            val text = buffer.readString()
            return RdAssertion(masterVersion, slaveVersion, text)
        }

        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: RdAssertion) {
            buffer.writeInt(value.masterVersion)
            buffer.writeInt(value.slaveVersion)
            buffer.writeString(value.text)
        }

    }
    //fields
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other::class != this::class) return false

        other as RdAssertion

        if (masterVersion != other.masterVersion) return false
        if (slaveVersion != other.slaveVersion) return false
        if (text != other.text) return false

        return true
    }
    //hash code trait
    override fun hashCode(): Int {
        var __r = 0
        __r = __r*31 + masterVersion.hashCode()
        __r = __r*31 + slaveVersion.hashCode()
        __r = __r*31 + text.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter) {
        printer.println("RdAssertion (")
        printer.indent {
            print("masterVersion = "); masterVersion.print(printer); println()
            print("slaveVersion = "); slaveVersion.print(printer); println()
            print("text = "); text.print(printer); println()
        }
        printer.print(")")
    }
}