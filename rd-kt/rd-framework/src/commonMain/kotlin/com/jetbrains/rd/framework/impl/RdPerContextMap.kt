package com.jetbrains.rd.framework.impl

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.util.addUnique
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.SequentialLifetimes
import com.jetbrains.rd.util.lifetime.onTermination
import com.jetbrains.rd.util.reactive.*

class RdPerContextMap<K: Any, V : RdBindableBase> private constructor(override val key: RdContext<K>, val valueFactory: (Boolean) -> V, private val myInternalMap: ViewableMap<K, V>) : RdReactiveBase(), IPerContextMap<K, V> {
    constructor(key: RdContext<K>, valueFactory: (Boolean) -> V) : this(key, valueFactory, ViewableMap())

    override fun deepClone(): IRdBindable {
        return RdPerContextMap(key, valueFactory)
    }

    private val myLocalValueSet = ViewableSet<K>()
    private val mySwitchingValueSet = SwitchingViewableSet(Lifetime.Eternal, myLocalValueSet)
    private val myUnboundLifetimes = SequentialLifetimes(Lifetime.Eternal)
    private val myUnboundValues = HashMap<K, V>()

    public val changing = myInternalMap.changing
    public var optimizeNested: Boolean = false

    override fun onWireReceived(buffer: AbstractBuffer) {
        // this entity has no own messages
        error("RdPerContextMap at $location received a message" )
    }

    init {
        bindToUnbounds()
    }

    private fun bindToUnbounds() {
        val unboundLt = myUnboundLifetimes.next()
        Signal.priorityAdviseSection {
            myLocalValueSet.view(unboundLt) { localKeyLt, localKey ->
                myUnboundValues.addUnique(localKeyLt, localKey, valueFactory(false))
            }
        }
    }

    override fun init(lifetime: Lifetime) {
        super.init(lifetime)
        val protocolValueSet = protocol.contexts.getProtocolValueSet(key)
        val keyHandler = protocol.contexts.getContextHandler(key)
        protocolValueSet.view(lifetime) { keyLt, key ->
            val previousUnboundValue = myUnboundValues[keyHandler.transformValueFromProtocol(key)]
            val newEntity = (previousUnboundValue ?: valueFactory(master)).withId(rdid.mix(key.toString()))
            newEntity.bind(keyLt, this, "[${key}]")
            myInternalMap.addUnique(keyLt, key, newEntity)
        }
        mySwitchingValueSet.changeBackingSet(protocol.contexts.getValueSet(key), true) // protocol set takes precedence
        myUnboundLifetimes.terminateCurrent() // also clears local set and unbound map
        lifetime.onTermination {
            mySwitchingValueSet.changeBackingSet(myLocalValueSet, false)
            bindToUnbounds() // re-create unbound entities (unlikely to happen)
        }
    }

    override fun view(lifetime: Lifetime, handler: (Lifetime, Map.Entry<K, V>) -> Unit) {
        mySwitchingValueSet.view(lifetime) { keyLt, localKey ->
            handler(keyLt, KeyValuePair(localKey, get(localKey)!!))
        }
    }

    override fun view(lifetime: Lifetime, handler: (Lifetime, K, V) -> Unit) {
        view(lifetime) { entryLt, entry ->
            handler(entryLt, entry.key, entry.value)
        }
    }

    override operator fun get(key: K): V? {
        if(!isBound) {
            if(!myLocalValueSet.contains(key))
                myLocalValueSet.add(key) // assume that all keys accessed before bind exist

            return myUnboundValues[key]
        }
        val protocolkey = protocol.contexts.getContextHandler(this.key).transformValueToProtocol(key) ?: return null
        return myInternalMap[protocolkey]
    }

    override fun getForCurrentContext(): V {
        val currentId = key.value ?: error("No ${key.key} set for getting value for it")
        return this[currentId] ?: error("No value in ${this.location} for ${key.key} = $currentId")
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