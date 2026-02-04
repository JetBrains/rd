package com.jetbrains.rd.framework

import com.jetbrains.rd.framework.FrameworkMarshallers.create
import com.jetbrains.rd.framework.base.IRdWireable
import com.jetbrains.rd.framework.impl.RdSignal
import com.jetbrains.rd.util.string.RName

object RNameMarshaller {
    fun read(buffer: AbstractBuffer): RName {
        val isEmpty = buffer.readBoolean()
        if (isEmpty)
            return RName.Empty

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

    fun write(buffer: AbstractBuffer, value: RName) {
        buffer.writeBoolean(value == RName.Empty)
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

internal fun IRdDynamic.createExtSignal(identities: IIdentities): RdSignal<ExtCreationInfo> {
    val marshaller = create(ExtCreationInfo::class, { buffer ->
            val rName = RNameMarshaller.read(buffer)
            val rdId = buffer.readNullable { buffer.readRdId() }
            val hash = buffer.readLong()
            ExtCreationInfo(rName, rdId, hash, null)
        },
        { buffer, (rName, rdId, hash) ->
            RNameMarshaller.write(buffer, rName)
            buffer.writeNullable(rdId) { buffer.writeRdId(it) }
            buffer.writeLong(hash)
        }
    )
    return RdSignal(marshaller).also {
        it.async = true

        val baseId = (this as? IRdWireable)?.rdid ?: RdId.Null
        it.rdid = identities.mix(baseId, "ProtocolExtCreated")
    }
}