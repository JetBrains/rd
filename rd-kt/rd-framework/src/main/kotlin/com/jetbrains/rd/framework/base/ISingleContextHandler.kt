package com.jetbrains.rd.framework.base

import com.jetbrains.rd.framework.AbstractBuffer
import com.jetbrains.rd.framework.RdContext
import com.jetbrains.rd.framework.SerializationCtx

internal interface ISingleContextHandler<T: Any> {
    val context: RdContext<T>

    @Suppress("UNCHECKED_CAST")
    fun writeValue(ctx: SerializationCtx, buffer: AbstractBuffer)

    fun readValue(ctx: SerializationCtx, buffer: AbstractBuffer): T?

    fun registerValueInValueSet()
}
