package com.jetbrains.rd.framework.base

import com.jetbrains.rd.framework.ISerializers
import com.jetbrains.rd.framework.Protocol
import com.jetbrains.rd.util.trace

interface ISerializersOwner {
    fun register(serializers : ISerializers) {
        val key = this::class
        if (!serializers.toplevels.add(key)) return

        Protocol.initializationLogger.trace { "REGISTER serializers for ${key.simpleName}" }
        registerSerializersCore(serializers)
    }

    fun registerSerializersCore(serializers : ISerializers)
}