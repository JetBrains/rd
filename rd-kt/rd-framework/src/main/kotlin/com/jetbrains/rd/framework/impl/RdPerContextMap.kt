package com.jetbrains.rd.framework.impl

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.onTermination
import com.jetbrains.rd.util.reactive.IViewableMap
import com.jetbrains.rd.util.reactive.ViewableMap

class RdPerContextMap<K: Any, V : RdBindableBase> private constructor(override val context: RdContext<K>, val valueFactory: (Boolean) -> V, private val internalMap: ViewableMap<K, V>) : RdReactiveBase(), IPerContextMap<K, V>, IViewableMap<K, V> by internalMap {
    constructor(context: RdContext<K>, valueFactory: (Boolean) -> V) : this(context, valueFactory, ViewableMap())

    override fun deepClone(): IRdBindable {
        return RdPerContextMap(context, valueFactory)
    }

    public val changing = internalMap.changing
    public var optimizeNested: Boolean = false

    override fun onWireReceived(buffer: AbstractBuffer) {
        // this entity has no own messages
        error("RdPerContextMap at $location received a message" )
    }

    override fun init(lifetime: Lifetime) {
        super.init(lifetime)
        val protocolValueSet = protocol.contexts.getValueSet(context)
        internalMap.keys.filter { !protocolValueSet.contains(it) }.forEach { internalMap.remove(it) }
        protocolValueSet.view(lifetime) { keyLt, key ->
            val oldValue = internalMap[key]
            val newEntity = (oldValue ?: valueFactory(master)).withId(rdid.mix(key.toString()))
            newEntity.bind(keyLt, this, "[${key}]")
            if (oldValue == null)
                internalMap[key] = newEntity
            keyLt.onTermination {
                newEntity.rdid = RdId.Null
            }
        }
    }

    override operator fun get(key: K): V? {
        if(!isBound) {
            if (!internalMap.containsKey(key))
                internalMap[key] = valueFactory(false)
        }
        return internalMap[key]
    }

    override fun getForCurrentContext(): V {
        val currentId = context.valueForPerContextEntity ?: error("No ${context.key} set for getting value for it")
        return this[currentId] ?: error("No value in ${this.location} for ${context.key} = $currentId")
    }

    companion object {
        fun <K: Any, V : RdBindableBase> read(key: RdContext<K>, buffer: AbstractBuffer, valueFactory : (Boolean) -> V) : RdPerContextMap<K, V> {
            val id = buffer.readRdId()
            return RdPerContextMap(key, valueFactory).withId(id)
        }

        fun write(buffer: AbstractBuffer, map: RdPerContextMap<*, *>) {
            buffer.writeRdId(map.rdid)
        }
    }
}