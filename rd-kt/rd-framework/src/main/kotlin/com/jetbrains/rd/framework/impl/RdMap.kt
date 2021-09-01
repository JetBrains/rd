package com.jetbrains.rd.framework.impl

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.framework.base.bindPolymorphic
import com.jetbrains.rd.framework.base.identifyPolymorphic
import com.jetbrains.rd.util.error
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.parseFromOrdinal
import com.jetbrains.rd.util.reactive.*
import com.jetbrains.rd.util.string.PrettyPrinter
import com.jetbrains.rd.util.string.condstr
import com.jetbrains.rd.util.string.print
import com.jetbrains.rd.util.string.printToString
import com.jetbrains.rd.util.trace


@Suppress("UNUSED_PARAMETER")
class RdMap<K : Any, V : Any> private constructor(
    val keySzr: ISerializer<K>,
    val valSzr: ISerializer<V>,
    private val map: ViewableMap<K, V>
) : RdReactiveBase(), IAsyncViewableMap<K, V>, IMutableViewableMap<K, V> by map {

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

    override fun init(lifetime: Lifetime) {
        super.init(lifetime)

        localChange { advise(lifetime) lambda@{
            if (!isLocalChange) return@lambda

            if (!optimizeNested) (it.newValueOpt)?.identifyPolymorphic(protocol.identity, protocol.identity.next(rdid))

            wire.send(rdid) { buffer ->
                val versionedFlag = (if (master) 1 else 0) shl versionedFlagShift
                val op = when (it) {
                    is IViewableMap.Event.Add ->    Op.Add
                    is IViewableMap.Event.Update -> Op.Update
                    is IViewableMap.Event.Remove -> Op.Remove
                }
                buffer.writeInt(versionedFlag or op.ordinal)

                val version = if (master) ++nextVersion else 0L

                if (master) {
                    pendingForAck.put(it.key, version)
                    buffer.writeLong(version)
                }

                keySzr.write(serializationContext, buffer, it.key)

                it.newValueOpt?.let { valSzr.write(serializationContext, buffer, it) }

                logSend.trace { logmsg(op, version, it.key, it.newValueOpt) }
            }
        }}

        wire.advise(lifetime, this)

        if (!optimizeNested)
            view(lifetime) { lf, entry -> (entry.value).bindPolymorphic(lf, this, "[${entry.key}]") }
    }


    override fun onWireReceived(buffer: AbstractBuffer) {
        val header = buffer.readInt()
        val msgVersioned = (header shr versionedFlagShift) != 0
        val op = parseFromOrdinal<Op>(header and ((1 shl versionedFlagShift) - 1))

        val version = if (msgVersioned) buffer.readLong() else 0

        val key = keySzr.read(serializationContext, buffer)

        if (op == Op.Ack) {
            val errmsg =
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

            if (errmsg.isEmpty())
                logReceived.trace  { logmsg(Op.Ack, version, key) }
            else
                logReceived.error {  logmsg(Op.Ack, version, key) + " >> $errmsg"}

        } else {
            val isPut = (op == Op.Add || op == Op.Update)
            val value = if (isPut) valSzr.read(serializationContext, buffer) else null

            if (msgVersioned || !master || !pendingForAck.containsKey(key)) {
                logReceived.trace { logmsg(op, version, key, value) }

                if (value != null) map[key] = value
                else map.remove(key)

            } else {
                logReceived.trace { logmsg(op, version, key, value) + " >> REJECTED" }
            }


            if (msgVersioned) {
                wire.send(rdid) { innerBuffer ->
                    innerBuffer.writeInt((1 shl versionedFlagShift) or Op.Ack.ordinal)
                    innerBuffer.writeLong(version)
                    keySzr.write(serializationContext, innerBuffer, key)

                    logSend.trace { logmsg(Op.Ack, version, key) }
                }

                if (master) logReceived.error { "Both ends are masters: $location" }
            }

        }
    }

    constructor(keySzr: ISerializer<K> = Polymorphic(), valSzr: ISerializer<V> = Polymorphic()) : this(keySzr, valSzr, ViewableMap())



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



    override fun put(key: K, value: V): V? = localChange { map.put(key, value) }
    override fun remove(key: K): V? = localChange { map.remove(key) }
    override fun clear() = localChange { map.clear() }


    override fun advise(lifetime: Lifetime, handler: (IViewableMap.Event<K, V>) -> Unit) {
        if (isBound) assertThreading()
        map.advise(lifetime, handler)
    }

    override fun adviseOn(lifetime: Lifetime, scheduler: IScheduler, handler: (IViewableMap.Event<K, V>) -> Unit) {
        if (isBound) assertThreading()
        map.advise(lifetime) { e -> scheduler.invokeOrQueue { handler(e) }}
    }
}
