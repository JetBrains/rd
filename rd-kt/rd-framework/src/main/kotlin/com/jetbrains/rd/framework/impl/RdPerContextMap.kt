package com.jetbrains.rd.framework.impl

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.util.collections.SynchronizedMap
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.isAlive
import com.jetbrains.rd.util.lifetime.isNotAlive
import com.jetbrains.rd.util.reactive.AddRemove
import com.jetbrains.rd.util.reactive.IViewableMap
import com.jetbrains.rd.util.reactive.ViewableMap
import kotlin.collections.ArrayDeque

class RdPerContextMap<K: Any, V : RdBindableBase> private constructor(override val context: RdContext<K>, val valueFactory: (Boolean) -> V, private val internalMap: ViewableMap<K, V>) : RdReactiveBase(), IPerContextMap<K, V>, IViewableMap<K, V> by internalMap {
    constructor(context: RdContext<K>, valueFactory: (Boolean) -> V) : this(context, valueFactory, ViewableMap(SynchronizedMap()))

    override fun deepClone(): IRdBindable {
        return RdPerContextMap(context, valueFactory)
    }

    val changing = internalMap.changing
    var optimizeNested: Boolean = false

    private var queue: ReactiveQueue<Pair<K, V>>? = null

    override fun onWireReceived(proto: IProtocol, buffer: AbstractBuffer, ctx: SerializationCtx, dispatchHelper: IRdWireableDispatchHelper) {
        // this entity has no own messages
        error("RdPerContextMap at $location received a message" )
    }

    override fun preInit(lifetime: Lifetime, proto: IProtocol) {
        super.preInit(lifetime, proto)

        val localQueue = ReactiveQueue<Pair<K, V>>()
        queue = localQueue


        val protocolValueSet = proto.contexts.getValueSet(context)
        proto.scheduler.invokeOrQueue {
            internalMap.keys.filter { !protocolValueSet.contains(it) }.forEach { internalMap.remove(it) }
        }

        protocolValueSet.view(lifetime) { contextValueLifetime, contextValue ->
            val oldValue = internalMap[contextValue]

            val value = (oldValue ?: valueFactory(master))

            contextValueLifetime.executeIfAlive {
                value.withId(proto.identity.mix(rdid, contextValue.toString()))
                value.preBind(contextValueLifetime, this, "[${contextValue}]")
            }

            localQueue.enqueue(Pair(contextValue, value))
        }
    }

    override fun init(lifetime: Lifetime, proto: IProtocol, ctx: SerializationCtx) {
        super.init(lifetime, proto, ctx)

        val localQueue = queue
        if (localQueue == null) {
            if (proto.lifetime.isNotAlive)
                return

            error("queue must not be null")
        }

        localQueue.view(lifetime) { pair ->
            lifetime.executeIfAlive {
                pair.second.bind()

                if (!internalMap.containsKey(pair.first)) {
                    proto.scheduler.invokeOrQueue {
                        if (!internalMap.containsKey(pair.first)) {
                            internalMap[pair.first] = pair.second
                        }
                    }
                }
            }
        }
    }

    override fun adviseAddRemove(lifetime: Lifetime, handler: (AddRemove, K, V) -> Unit) {
        assertThreading()
        internalMap.adviseAddRemove(lifetime, handler)
    }

    override fun view(lifetime: Lifetime, handler: (Lifetime, Map.Entry<K, V>) -> Unit) {
        assertThreading()
        internalMap.view(lifetime, handler)
    }

    override fun view(lifetime: Lifetime, handler: (Lifetime, K, V) -> Unit) {
        assertThreading()
        internalMap.view(lifetime, handler)
    }

    override operator fun get(key: K): V? {
        if(!isBound) {
            if (!internalMap.containsKey(key))
                internalMap[key] = valueFactory(false)
        }

        assertThreading()
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

    private class ReactiveQueue<T> {
        private var listener: ((T) -> Unit)? = null
        private val queue = ArrayDeque<T>()

        fun enqueue(value: T) {
            val action: (T) -> Unit
            synchronized(queue) {
                val localListener = listener
                if (localListener != null) {
                    assert(queue.size == 0)
                    action = localListener
                } else{
                    queue.add(value)
                    return
                }
            }

            action(value)
        }

        fun view(lifetime: Lifetime, action: (T) -> Unit) {
            while (lifetime.isAlive) {
                val value = synchronized(queue) {
                    if (queue.size > 0) {
                        if (lifetime.isNotAlive)
                            return

                        queue.removeFirst()
                    } else {
                        lifetime.bracketIfAlive({
                            assert(listener == null)
                            listener = action
                        }, {
                            synchronized(queue) {
                                listener = null
                            }
                        })

                        return
                    }
                }

                action(value)
            }
        }
    }
}