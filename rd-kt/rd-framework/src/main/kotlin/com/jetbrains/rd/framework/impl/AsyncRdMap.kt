package com.jetbrains.rd.framework.impl

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.reactive.IViewableMap
import com.jetbrains.rd.util.string.RName
import com.jetbrains.rd.util.threading.SynchronousScheduler
import java.util.function.BiFunction
import java.util.function.Function

class AsyncRdMap<K : Any, V : Any> private constructor(
    private val map: BackendRdMap<K, V>
) : IRdBindable,  MutableMap<K, V>, IAsyncSource2<IViewableMap.Event<K, V>> {

    constructor(keySzr: ISerializer<K> = Polymorphic(), valSzr: ISerializer<V> = Polymorphic(),) : this(BackendRdMap(keySzr, valSzr))

    companion object : ISerializer<AsyncRdMap<*, *>> {
        fun<K:Any, V:Any> read(ctx: SerializationCtx, buffer: AbstractBuffer, keySzr: ISerializer<K>, valSzr: ISerializer<V>): AsyncRdMap<K, V> = AsyncRdMap(keySzr, valSzr).withId(RdId.read(buffer))
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: AsyncRdMap<*, *>) = value.rdid.write(buffer)

        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): AsyncRdMap<*, *> {
            return read(ctx, buffer, Polymorphic(), Polymorphic())
        }
    }

    val change: IAsyncSource2<IViewableMap.Event<K, V>> = object : IAsyncSource2<IViewableMap.Event<K, V>> {
        override fun adviseOn(lifetime: Lifetime, scheduler: IScheduler, handler: (IViewableMap.Event<K, V>) -> Unit) {
            map.change.advise(lifetime) {
                scheduler.queue { handler(it) }
            }
        }
    }

    override fun adviseOn(lifetime: Lifetime, scheduler: IScheduler, handler: (IViewableMap.Event<K, V>) -> Unit) {
        synchronized(map) {
            map.advise(lifetime) { event ->
                scheduler.queue { handler(event) }
            }
        }
    }

    fun withId(id: RdId) : AsyncRdMap<K, V> {
        synchronized(map ) {
            map.withId(id)
        }
        return this
    }

    var optimizeNested: Boolean
        get() = true
        set(value) {}

    var async: Boolean
        get() = true
        set(value) {}

    override val rdid: RdId
        get() = synchronized(map) {
            map.rdid
        }

    override fun preBind(lf: Lifetime, parent: IRdDynamic, name: String) {
        synchronized(map) {
            map.preBind(lf, parent, name)
        }
    }

    override fun bind() {
        synchronized(map) {
            map.bind()
        }
    }

    override fun identify(identities: IIdentities, id: RdId) {
        synchronized(map) {
            map.identify(identities, id)
        }
    }

    override fun deepClone(): IRdBindable {
        synchronized(map) {
            val clone = map.deepClone()
            return AsyncRdMap(clone)
        }
    }

    override val protocol: IProtocol?
        get() = map.protocol

    override val serializationContext: SerializationCtx?
        get() = map.serializationContext

    override val location: RName
        get() = map.location

    override val size: Int
        get() = map.size

    override fun containsKey(key: K): Boolean {
        return map.containsKey(key)
    }

    override fun containsValue(value: V): Boolean {
        return map.containsValue(value)
    }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = map.entries // todo return wrapper to synchronize modification operations

    override val keys: MutableSet<K>
        get() = map.keys  // todo return wrapper to synchronize modification operations

    override val values: MutableCollection<V>
        get() = map.values  // todo return wrapper to synchronize modification operations

    override fun clear() {
        synchronized(map) {
            map.clear()
        }
    }

    override fun compute(key: K, remappingFunction: BiFunction<in K, in V?, out V?>): V? {
        TODO()
    }

    override fun computeIfAbsent(key: K, mappingFunction: Function<in K, out V>): V {
        TODO()
    }

    override fun computeIfPresent(key: K, remappingFunction: BiFunction<in K, in V, out V?>): V? {
        TODO()
    }

    override fun get(key: K): V? = map[key]

    override fun getOrDefault(key: K, defaultValue: V): V {
        return map.getOrDefault(key,defaultValue)
    }

    override fun isEmpty(): Boolean {
        return map.isEmpty()
    }

    override fun merge(key: K, value: V, remappingFunction: BiFunction<in V, in V, out V?>): V? {
        TODO()
    }

    override fun put(key: K, value: V): V? {
        return synchronized(map) {
            map.put(key, value)
        }
    }

    override fun putAll(from: Map<out K, V>) {
        synchronized(map) {
            map.putAll(from)
        }
    }

    override fun putIfAbsent(key: K, value: V): V? {
        return synchronized(map) {
            map.putIfAbsent(key, value)
        }
    }

    override fun remove(key: K): V? {
        return synchronized(map) {
            map.remove(key)
        }
    }

    override fun remove(key: K, value: V): Boolean {
        return synchronized(map) {
            map.remove(key, value)
        }
    }

    override fun replace(key: K, oldValue: V, newValue: V): Boolean {
        return synchronized(map) {
            map.replace(key, oldValue, newValue)
        }
    }

    override fun replace(key: K, value: V): V? {
        return synchronized(map) {
            map.replace(key, value)
        }
    }

    override fun replaceAll(function: BiFunction<in K, in V, out V>) {
        TODO()
    }

    private class BackendRdMap<K : Any, V : Any>(
        keySzr: ISerializer<K> = Polymorphic(),
        valSzr: ISerializer<V> = Polymorphic(),
    ) : RdMap<K, V>(keySzr, valSzr) {

        init {
            async = true
            optimizeNested = true
        }

        override fun assertBindingThread() {
        }

        override fun assertThreading() {
        }

        override fun onWireReceived(proto: IProtocol, buffer: AbstractBuffer, ctx: SerializationCtx, dispatchHelper: IRdWireableDispatchHelper) {
            synchronized(this) {
                super.onWireReceived(proto, buffer, ctx, object : IRdWireableDispatchHelper {
                    override val rdId: RdId
                        get() = dispatchHelper.rdId
                    override val lifetime: Lifetime
                        get() = dispatchHelper.lifetime

                    override fun dispatch(lifetime: Lifetime, scheduler: IScheduler?, action: () -> Unit) {
                        dispatchHelper.dispatch(lifetime, SynchronousScheduler, action)
                    }
                })
            }
        }

        override fun deepClone(): BackendRdMap<K, V> = BackendRdMap(keySzr, valSzr).also { for ((k,v) in it) { it[k] = v.deepClonePolymorphic() } }
    }
}