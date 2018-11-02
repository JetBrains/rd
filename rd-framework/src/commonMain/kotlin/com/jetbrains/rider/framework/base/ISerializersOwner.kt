package com.jetbrains.rider.framework.base

import com.jetbrains.rider.framework.ISerializers
import com.jetbrains.rider.framework.Protocol
import com.jetbrains.rider.util.trace

interface ISerializersOwner {
    fun register(serializers : ISerializers) {
        val key = this::class
        if (!serializers.toplevels.add(key)) return

        Protocol.initializationLogger.trace { "REGISTER serializers for ${key.simpleName}" }
        registerSerializersCore(serializers)
    }

    fun registerSerializersCore(serializers : ISerializers)
}