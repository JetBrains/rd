package com.jetbrains.rd.framework.impl

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.util.addUnique
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.*

class RdPerContextMap<K: Any, V : RdBindableBase> private constructor(override val key: RdContextKey<K>, val valueFactory: (Boolean) -> V, private val myInternalMap: ViewableMap<K, V>) : RdReactiveBase(), IPerContextMap<K, V> {
    constructor(key: RdContextKey<K>, valueFactory: (Boolean) -> V) : this(key, valueFactory, ViewableMap())

    override fun deepClone(): IRdBindable {
        return RdPerContextMap(key, valueFactory)
    }

    public val changing = myInternalMap.changing
    public var optimizeNested: Boolean = false

    override fun onWireReceived(buffer: AbstractBuffer) {
        // this entity has no own messages
        error("RdPerContextMap at $location received a message" )
    }

    override fun init(lifetime: Lifetime) {
        super.init(lifetime)
        protocol.contextHandler.getProtocolValueSet(key).view(lifetime) { keyLt, key ->
            val newEntity = valueFactory(master).withId(rdid.mix(key.toString()))
            newEntity.bind(keyLt, this, "[${key}]")
            myInternalMap.addUnique(keyLt, key, newEntity)
        }
    }

    override fun view(lifetime: Lifetime, handler: (Lifetime, Map.Entry<K, V>) -> Unit) {
        protocol.contextHandler.getValueSet(key).view(lifetime) { keyLt, localKey ->
            handler(keyLt, KeyValuePair(localKey, get(localKey)!!))
        }
    }

    override fun view(lifetime: Lifetime, handler: (Lifetime, K, V) -> Unit) {
        view(lifetime) { entryLt, entry ->
            handler(entryLt, entry.key, entry.value)
        }
    }

    override operator fun get(key: K): V? {
        val protocolkey = protocol.contextHandler.getKeyHandler(this.key).transformValueToProtocol(key) ?: return null
        return myInternalMap[protocolkey]
    }

    @Suppress("UNCHECKED_CAST")
    override fun getForCurrentContext(): V {
        val currentId = key.value ?: error("No ${key.key} set for getting value for it")
        return this[currentId as K] ?: error("No value in ${this.location} for ${key.key} = $currentId")
    }

    companion object {
        fun <K: Any, V : RdBindableBase> read(key: RdContextKey<K>, buffer: AbstractBuffer, valueFactory : (Boolean) -> V) : RdPerContextMap<K, V> {
            val id = buffer.readRdId()
            return RdPerContextMap(key, valueFactory).withId(id)
        }

        fun write(buffer: AbstractBuffer, map: RdPerContextMap<*, *>) {
            buffer.writeRdId(map.rdid)
        }
    }
}