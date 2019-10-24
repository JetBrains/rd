package com.jetbrains.rd.framework.base

import com.jetbrains.rd.framework.AbstractBuffer
import com.jetbrains.rd.framework.RdContext
import com.jetbrains.rd.framework.SerializationCtx
import com.jetbrains.rd.framework.impl.ContextValueTransformer
import com.jetbrains.rd.framework.impl.ContextValueTransformerDirection

internal interface ISingleContextHandler<T: Any> {
    val key: RdContext<T>

    var myValueTransformer : ContextValueTransformer<T>?

    @Suppress("UNCHECKED_CAST")
    fun writeValue(ctx: SerializationCtx, buffer: AbstractBuffer)

    fun readValue(ctx: SerializationCtx, buffer: AbstractBuffer): T?

    @Suppress("UNCHECKED_CAST")
    fun getValueTransformed(): T? {
        return transformValueToProtocol(key.value)
    }

    fun transformValueToProtocol(valueRaw: T?): T? {
        val transformer = myValueTransformer
        return if(transformer != null) transformer(valueRaw, ContextValueTransformerDirection.WriteToProtocol) else valueRaw
    }

    fun transformValueFromProtocol(value: T?): T? {
        val transformer = myValueTransformer
        return if(transformer != null)
            transformer(value, ContextValueTransformerDirection.ReadFromProtocol)
        else
            value
    }
}
