package com.jetbrains.rd.framework

import com.jetbrains.rd.framework.base.IRdWireable
import com.jetbrains.rd.framework.impl.RdSignal
import com.jetbrains.rd.util.string.RName

object RNameMarshaller : IMarshaller<RName> {
    override val _type = RName::class
    
    override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): RName {
        val rootName = buffer.readString()
        var last = buffer.readBoolean()
        var rName = RName(rootName)
        while (!last) {
            val separator = buffer.readString()
            val localName = buffer.readString()
            last = buffer.readBoolean()
            rName = rName.sub(localName, separator)
        }
        return rName
    }

    override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: RName) {
        traverseRName(value) { rName, last ->
            if (rName == RName.Empty) return@traverseRName
            if (rName.parent != RName.Empty) {
                buffer.writeString(rName.separator)
            }
            buffer.writeString(rName.localName)
            buffer.writeBoolean(last)
        }
    }
    
    private fun traverseRName(rName: RName, last: Boolean = true, handler: (RName, last: Boolean) -> Unit) {
        rName.parent?.let { traverseRName(it, false, handler) }
        handler(rName, last)
    }
}

internal fun IRdDynamic.createExtSignal(): RdSignal<Triple<RName, RdId?, Long>> {
    val marshaller = FrameworkMarshallers.create(
        { buffer ->
            val rName = RNameMarshaller.read(serializationContext, buffer)
            val rdId = buffer.readNullable { buffer.readRdId() }
            val hash = buffer.readLong()
            Triple(rName, rdId, hash)
        },
        { buffer, (rName, rdId, hash) ->
            RNameMarshaller.write(serializationContext, buffer, rName)
            buffer.writeNullable(rdId) { buffer.writeRdId(it) }
            buffer.writeLong(hash)
        }
    )
    return RdSignal(marshaller).also {
        val baseId = (this as? IRdWireable)?.rdid ?: RdId.Null
        it.rdid = baseId.mix("ProtocolExtCreated")
    }
}