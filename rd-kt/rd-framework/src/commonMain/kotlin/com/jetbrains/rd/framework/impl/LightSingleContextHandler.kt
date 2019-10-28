package com.jetbrains.rd.framework.impl

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.IRdBindable
import com.jetbrains.rd.framework.base.ISingleContextHandler
import com.jetbrains.rd.framework.base.RdBindableBase

internal class LightSingleContextHandler<T: Any>(override val context: RdContext<T>, val serializer: ISerializer<T>) : RdBindableBase(), ISingleContextHandler<T> {
    override fun deepClone(): IRdBindable {
        error("This may not be cloned")
    }

    @Suppress("UNCHECKED_CAST")
    override fun writeValue(ctx: SerializationCtx, buffer: AbstractBuffer) {
        val value = context.value
        if(value == null)
            buffer.writeBoolean(false)
        else {
            buffer.writeBoolean(true)
            serializer.write(ctx, buffer, value)
        }
    }

    override fun readValue(ctx: SerializationCtx, buffer: AbstractBuffer): T? {
        val hasValue = buffer.readBoolean()
        if(!hasValue) return null
        return serializer.read(ctx, buffer)
    }
}