package com.jetbrains.rd.framework

import com.jetbrains.rd.framework.impl.RdMap
import com.jetbrains.rd.util.addUnique
import com.jetbrains.rd.util.lifetime.Lifetime

fun <T : Any> RdMap<ClientId, T>.getForCurrentClientId(): T {
    val currentId = ClientId.current ?: error("No ClientId")
    return this[currentId] ?: error("No value in ${this.location} for ClientId $currentId")
}

fun <T : Any> RdMap<ClientId, T>.adviseForProtocolClientIds(lifetime: Lifetime, valueFactory: () -> T) {
    protocol.clientIdSet.view(lifetime) { clientIdLt, clientId ->
        this.addUnique(clientIdLt, clientId, valueFactory())
    }
}
