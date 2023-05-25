package com.jetbrains.rd.framework.impl

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.util.*
import com.jetbrains.rd.util.collections.SynchronizedMap
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.lifetime.isAlive
import com.jetbrains.rd.util.reactive.*
import com.jetbrains.rd.util.string.*
import kotlin.assert


@Suppress("UNUSED_PARAMETER")
open class RdMap<K : Any, V : Any> private constructor(
    val keySzr: ISerializer<K>,
    val valSzr: ISerializer<V>,
    private val map: ViewableMap<K, V>
) : RdReactiveBase(), IAsyncViewableMap<K, V>, IMutableViewableMap<K, V> {

    companion object : ISerializer<RdMap<*, *>> {
        private enum class Op {Add, Update, Remove, Ack}

//        override val _type : Class<*> get() = throw IllegalStateException("Mustn't be used for polymorphic marshalling")
        fun<K:Any, V:Any> read(ctx: SerializationCtx, buffer: AbstractBuffer, keySzr: ISerializer<K>, valSzr: ISerializer<V>): RdMap<K, V> = RdMap(keySzr, valSzr).withId(RdId.read(buffer))
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: RdMap<*, *>) = value.rdid.write(buffer)

        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): RdMap<*, *> {
            return read(ctx, buffer, Polymorphic<Any>(), Polymorphic<Any>())
        }

        const val versionedFlagShift = 8
    }

    override fun deepClone(): IRdBindable = RdMap(keySzr, valSzr).also { for ((k,v) in map) { it[k] = v.deepClonePolymorphic() } }

    private var nextVersion = 0L
    private val pendingForAck = mutableMapOf<K, Long>()
    private var bindDefinitions: MutableMap<K, LifetimeDefinition?>? = null

    var optimizeNested: Boolean = false
        set(value) {
            field = value
            if (value) master = false
        }

    private fun logmsg(op: Op, version: Long, key: K, value: V? = null) : String {
        return "map `$location` ($rdid) :: ${op.name} :: key = ${key.printToString()}"+
            (version > 0).condstr   { " :: version = $version" }  +
            (value != null).condstr { " :: value = ${value.printToString()}" }
    }

    override fun preInit(lifetime: Lifetime, proto: IProtocol) {
        super.preInit(lifetime, proto)

        if (!optimizeNested) {
            val definitions = SynchronizedMap<K, LifetimeDefinition?>()

            for ((key, value) in this) {
                if (value != null) {
                    value.identifyPolymorphic(proto.identity, proto.identity.next(rdid))
                    val definition = tryPreBindValue(lifetime, key, value, false)
                    if (definition != null)
                        definitions[key] = definition
                }
            }

            lifetime.executeIfAlive {
                assert(bindDefinitions == null)
                bindDefinitions = definitions
            }
        }

        proto.wire.advise(lifetime, this)
    }

    override fun init(lifetime: Lifetime, proto: IProtocol, ctx: SerializationCtx) {
        super.init(lifetime, proto, ctx)

        if (!optimizeNested) {
            change.advise(lifetime) {
                if (isLocalChange) {
                    val definitions = tryGetBindDefinitions(lifetime) ?: return@advise

                    if (it !is IViewableMap.Event.Add)
                        definitions[it.key]?.terminate()

                    if (it !is IViewableMap.Event.Remove) {
                        val value = it.newValueOpt
                        value.identifyPolymorphic(proto.identity, proto.identity.next(rdid))
                        val definition = tryPreBindValue(lifetime, it.key, value, false)
                        definitions.put(it.key, definition)?.terminate()
                    }
                }
            }
        }

        localChange { advise(lifetime) lambda@{
            if (!isLocalChange) return@lambda

            proto.wire.send(rdid) { buffer ->
                val versionedFlag = (if (master) 1 else 0) shl versionedFlagShift
                val op = when (it) {
                    is IViewableMap.Event.Add ->    Op.Add
                    is IViewableMap.Event.Update -> Op.Update
                    is IViewableMap.Event.Remove -> Op.Remove
                }
                buffer.writeInt(versionedFlag or op.ordinal)

                val version = if (master) ++nextVersion else 0L

                if (master) {
                    Sync.lock(pendingForAck) {
                        pendingForAck.put(it.key, version)
                    }

                    buffer.writeLong(version)
                }

                keySzr.write(ctx, buffer, it.key)

                it.newValueOpt?.let { valSzr.write(ctx, buffer, it) }

                logSend.trace { logmsg(op, version, it.key, it.newValueOpt) }
            }

            if (!optimizeNested)
                it.newValueOpt.bindPolymorphic()
        }}
    }

    override fun findByRName(rName: RName): RdBindableBase? {
        if (rName == RName.Empty) return this
        val rootName = rName.getNonEmptyRoot()
        val localName = rootName.localName
        if (!localName.startsWith('[') || !localName.endsWith(']'))
            return null

        val stringKey = localName.removeSurrounding("[", "]")

        val entry = map.entries.find { (key, _) -> key.toString() == stringKey }
        val value = entry?.value as? RdBindableBase 
            ?: return null

        if (rootName == rName)
            return value

        return value.findByRName(rName.dropNonEmptyRoot())
    }

    override fun onWireReceived(proto: IProtocol, buffer: AbstractBuffer, ctx: SerializationCtx, dispatchHelper: IRdWireableDispatchHelper) {
        val header = buffer.readInt()
        val msgVersioned = (header shr versionedFlagShift) != 0
        val op = parseFromOrdinal<Op>(header and ((1 shl versionedFlagShift) - 1))

        val version = if (msgVersioned) buffer.readLong() else 0

        val key = keySzr.read(ctx, buffer)

        if (op == Op.Ack) {
            dispatchHelper.dispatch {
                val errmsg = Sync.lock(pendingForAck) {
                    if (!msgVersioned) "Received ${Op.Ack} while msg hasn't versioned flag set"
                    else if (!master) "Received ${Op.Ack} when not a Master"
                    else pendingForAck[key]?.let { pendingVersion ->
                        if (pendingVersion < version) "Pending version `$pendingVersion` < ${Op.Ack} version `$version`"
                        else {
                            //side effect
                            if (pendingVersion == version) pendingForAck.remove(key) //else we don't need to remove, silently drop
                            "" //return good result
                        }
                    } ?: "No pending for ${Op.Ack}"
                }

                if (errmsg.isEmpty())
                    logReceived.trace { logmsg(Op.Ack, version, key) }
                else
                    logReceived.error { logmsg(Op.Ack, version, key) + " >> $errmsg" }
            }

        } else {
            val isPut = (op == Op.Add || op == Op.Update)
            val value = if (isPut) valSzr.read(ctx, buffer) else null

            val lifetime = dispatchHelper.lifetime
            val definition = tryPreBindValue(lifetime, key, value, true)

            dispatchHelper.dispatch {
                if (msgVersioned || !master || !isPendingForAck(key)) {
                    logReceived.trace { logmsg(op, version, key, value) }

                    if (value != null) {
                        val definitions = tryGetBindDefinitions(lifetime)
                        if (definitions != null) {
                            if (op == Op.Update)
                                definitions[key]?.terminate()

                            definitions[key] = definition
                        }

                        map[key] = value
                    } else {
                        val prevDef = tryGetBindDefinitions(lifetime)?.remove(key)
                        prevDef?.terminate()
                        map.remove(key)
                    }

                } else {
                    logReceived.trace { logmsg(op, version, key, value) + " >> REJECTED" }
                }

                if (msgVersioned) {
                    proto.wire.send(rdid) { innerBuffer ->
                        innerBuffer.writeInt((1 shl versionedFlagShift) or Op.Ack.ordinal)
                        innerBuffer.writeLong(version)
                        keySzr.write(ctx, innerBuffer, key)

                        logSend.trace { logmsg(Op.Ack, version, key) }
                    }

                    if (master) logReceived.error { "Both ends are masters: $location" }
                }
            }
        }
    }

    private fun isPendingForAck(key: K): Boolean {
        Sync.lock(pendingForAck) {
            return pendingForAck.containsKey(key)
        }
    }

    private fun tryGetBindDefinitions(lifetime: Lifetime): MutableMap<K, LifetimeDefinition?>?  {
        val definitions = bindDefinitions
        return if (lifetime.isAlive) definitions else null
    }

    private fun tryPreBindValue(lifetime: Lifetime, key: K, value: V?, bindAlso: Boolean): LifetimeDefinition? {
        if (optimizeNested || value == null)
            return null

        val definition = LifetimeDefinition().apply { id = value }
        try {
            value.preBindPolymorphic(definition.lifetime, this, "[$key]")
            if (bindAlso)
                value.bindPolymorphic()

            (lifetime as LifetimeDefinition).attach(definition, true)

            return definition
        } catch (e: Throwable) {
            definition.terminate()
            throw e
        }
    }


    constructor(keySzr: ISerializer<K> = Polymorphic(), valSzr: ISerializer<V> = Polymorphic()) : this(keySzr, valSzr, ViewableMap(SynchronizedMap() /*to have thread=safe print*/))



    override fun print(printer: PrettyPrinter) {
        super.print(printer)
        printer.print(" [")
        if (!isEmpty()) printer.println()

        printer.indent {
            forEach {
                val (key, value) = it
                key.print(printer)
                printer.print(" => ")
                value.print(printer)
                printer.println()
            }
        }
        printer.print("]")
    }

    override val size: Int
        get() = map.size

    override fun containsKey(key: K): Boolean = map.containsKey(key)

    override fun containsValue(value: V): Boolean = map.containsValue(value)

    override fun get(key: K): V? = map.get(key)

    override fun isEmpty(): Boolean = map.isEmpty()

    override val entries
        get() = map.entries
    override val keys
        get() = map.keys
    override val values
        get() = map.values

    override val change: ISource<IViewableMap.Event<K, V>>
        get() = map.change

    override fun put(key: K, value: V): V? = localChange { map.put(key, value) }
    override fun remove(key: K): V? = localChange { map.remove(key) }
    override fun remove(key: K, value: V): Boolean = localChange {
        if (this[key] == value) {
            remove(key)
            return@localChange true
        } else {
            return@localChange false
        }
    }

    override fun clear() = localChange { map.clear() }
    override fun putAll(from: Map<out K, V>) = localChange { map.putAll(from) }


    override fun advise(lifetime: Lifetime, handler: (IViewableMap.Event<K, V>) -> Unit) {
        if (isBound) assertThreading()
        map.advise(lifetime, handler)
    }

    override fun adviseOn(lifetime: Lifetime, scheduler: IScheduler, handler: (IViewableMap.Event<K, V>) -> Unit) {
        if (isBound) assertThreading()
        map.advise(lifetime) { e -> scheduler.invokeOrQueue { handler(e) }}
    }
}
