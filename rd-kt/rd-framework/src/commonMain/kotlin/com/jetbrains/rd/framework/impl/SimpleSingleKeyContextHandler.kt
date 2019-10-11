package com.jetbrains.rd.framework.impl

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.IRdBindable
import com.jetbrains.rd.framework.base.ISingleKeyProtocolContextHandler
import com.jetbrains.rd.framework.base.RdBindableBase

internal class SimpleSingleKeyContextHandler<T: Any>(override val key: RdContextKey<T>, val serializer: ISerializer<T>) : RdBindableBase(), ISingleKeyProtocolContextHandler<T> {
    override var myValueTransformer : ContextValueTransformer<T>? = null

    override fun deepClone(): IRdBindable {
        error("This may not be cloned")
    }

    @Suppress("UNCHECKED_CAST")
    override fun writeValue(ctx: SerializationCtx, writer: AbstractBuffer) {
        val value = getValueTransformed()
        if(value == null)
            writer.writeBoolean(false)
        else {
            writer.writeBoolean(true)
            serializer.write(ctx, writer, value)
        }
    }

    override fun readValue(ctx: SerializationCtx, reader: AbstractBuffer): T? {
        val hasValue = reader.readBoolean()
        if(!hasValue) return null
        return transformValueFromProtocol(serializer.read(ctx, reader))
    }
}