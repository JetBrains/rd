package com.jetbrains.rd.framework

import com.jetbrains.rd.util.ConcurrentHashMap
import com.jetbrains.rd.util.addUnique
import com.jetbrains.rd.util.lifetime.Lifetime

class RdEntitiesRegistrar {
    private val map = ConcurrentHashMap<RdId, IRdDynamic>()

    internal fun register(lifetime: Lifetime, rdId: RdId, dynamic: IRdDynamic) {
        require(!rdId.isNull)
        map.addUnique(lifetime, rdId, dynamic)
    }

    fun tryGetDynamic(rdId: RdId): IRdDynamic? = map[rdId]
}