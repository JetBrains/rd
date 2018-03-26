package com.jetbrains.rider.framework.text.intrinsics

import com.jetbrains.rider.framework.*
import com.jetbrains.rider.framework.AbstractBuffer
import com.jetbrains.rider.util.ot.*
import kotlin.reflect.*

@Suppress("unused")
object OtOperationMarshaller : IMarshaller<OtOperation> {
    private const val RetainCode: Byte = 1
    private const val InsertCode: Byte = 2
    private const val DeleteCode: Byte = 3

    override val _type: KClass<*>
        get() = OtOperation::class

    override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): OtOperation {
        val changes = buffer.readList {
            val id = buffer.readByte()
            when (id) {
                RetainCode -> {
                    val offset = buffer.readInt()
                    Retain(offset)
                }
                InsertCode -> {
                    val text = buffer.readString()
                    InsertText(text)
                }
                DeleteCode -> {
                    val text = buffer.readString()
                    DeleteText(text)
                }
                else -> throw IllegalStateException("Can't find reader by id: " + this.id.toString())
            }
        }
        val role = buffer.readEnum<OtRole>()
        return OtOperation(changes, role)
    }

    override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: OtOperation) {
        buffer.writeList(value.changes) { v ->
            when(v) {
                is Retain -> {
                    buffer.writeByte(RetainCode)
                    buffer.writeInt(v.offset)
                }
                is InsertText -> {
                    buffer.writeByte(InsertCode)
                    buffer.writeString(v.text)
                }
                is DeleteText -> {
                    buffer.writeByte(DeleteCode)
                    buffer.writeString(v.text)
                }
            }
        }
        buffer.writeEnum(value.role)
    }
}