package com.jetbrains.rd.framework

import com.jetbrains.rd.framework.base.RdReactiveBase
import com.jetbrains.rd.framework.impl.RdPerClientIdMap

fun <T : RdReactiveBase> RdPerClientIdMap<T>.getForCurrentClientId(): T {
    val currentId = ClientId.current
    return this[currentId] ?: error("No value in ${this.location} for ClientId $currentId")
}
