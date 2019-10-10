package com.jetbrains.rd.framework.impl

import com.jetbrains.rd.framework.AbstractBuffer
import com.jetbrains.rd.framework.ClientId
import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.framework.readRdId
import com.jetbrains.rd.framework.writeRdId
import com.jetbrains.rd.util.addUnique
import com.jetbrains.rd.util.assert
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IViewableMap
import com.jetbrains.rd.util.reactive.ViewableMap

class RdPerClientIdMap<V : RdBindableBase> private constructor(val valueFactory: (Boolean) -> V, private val myInternalMap: ViewableMap<ClientId, V>) : RdReactiveBase(), IViewableMap<ClientId, V> by myInternalMap {
    constructor(valueFactory: (Boolean) -> V) : this(valueFactory, ViewableMap())

    val changing = myInternalMap.changing
    var optimizeNested: Boolean = false

    override fun deepClone(): IRdBindable = RdPerClientIdMap(valueFactory).also { for ((k,v) in myInternalMap) { it.myInternalMap[k] = v.deepClonePolymorphic() } }

    override fun onWireReceived(buffer: AbstractBuffer) {
        // this entity has no own messages
        assert(false) { "RdPerClientIdMap at $location received a message" }
    }

    override fun init(lifetime: Lifetime) {
        super.init(lifetime)
        protocol.clientIdSet.view(lifetime) { clientIdLt, clientId ->
            val newEntity = valueFactory(master).withId(rdid.mix(clientId.value))
            newEntity.bind(clientIdLt, this, "$[{clientId.value}]")
            myInternalMap.addUnique(clientIdLt, clientId, newEntity)
        }
    }

    companion object {
        fun <V : RdBindableBase> read(buffer: AbstractBuffer, valueFactory : (Boolean) -> V) : RdPerClientIdMap<V> {
            val id = buffer.readRdId()
            return RdPerClientIdMap(valueFactory).withId(id)
        }

        fun write(buffer: AbstractBuffer, map: RdPerClientIdMap<*>) {
            buffer.writeRdId(map.rdid)
        }
    }
}