package com.jetbrains.rd.framework.base

import com.jetbrains.rd.framework.ISerializers
import com.jetbrains.rd.framework.Protocol
import com.jetbrains.rd.util.trace

interface ISerializersOwner {
    fun register(serializers : ISerializers) {
        serializers.registerSerializersOwnerOnce(this)
    }

    fun registerSerializersCore(serializers : ISerializers)
}