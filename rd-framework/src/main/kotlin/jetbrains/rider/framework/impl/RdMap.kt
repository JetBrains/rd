package com.jetbrains.rider.framework.impl

import com.jetbrains.rider.framework.*
import com.jetbrains.rider.framework.base.*
import com.jetbrains.rider.util.condstr
import com.jetbrains.rider.util.error
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.parseFromOrdinal
import com.jetbrains.rider.util.reactive.*
import com.jetbrains.rider.util.string.PrettyPrinter
import com.jetbrains.rider.util.trace
import java.io.InputStream
import java.io.OutputStream



class RdMap<K : Any, V : Any> private constructor(val keySzr: ISerializer<K>, val valSzr: ISerializer<V>, private val map: ViewableMap<K,V>)
: RdReactiveBase(), IAsyncViewableMap<K, V>, IMutableViewableMap<K, V> by map {

    companion object {
        private enum class Op {Add, Update, Remove, Ack}

//        override val _type : Class<*> get() = throw IllegalStateException("Mustn't be used for polymorphic marshalling")
        fun<K:Any, V:Any> read(ctx: SerializationCtx, stream: InputStream, keySzr: ISerializer<K>, valSzr: ISerializer<V>): RdMap<K, V> = RdMap(keySzr, valSzr).withId(RdId.read(stream))
        fun write(ctx: SerializationCtx, stream: OutputStream, value: RdMap<*, *>) = value.id.write(stream)

        const val versionedFlagShift = 8
    }

    var nextVersion = 0L
    val pendingForAck = mutableMapOf<K, Long>()
    val master: Boolean get() = !optimizeNested //todo do it correct


    var optimizeNested: Boolean = false


    private fun logmsg(op: Op, version: Long, key: K, value: V? = null) : String {
        return "map `${location()}` ($id) :: ${op.name} :: key = ${key.printToString()}"+
            (version > 0).condstr   { " :: version = $version" }  +
            (value != null).condstr { " :: value = ${value.printToString()}" }
    }

    override fun init(lifetime: Lifetime) {
        super.init(lifetime)
        map.name = name

        val serializationContext = serializationContext

        localChange { advise(lifetime) lambda@{
            if (!isLocalChange) return@lambda

            if (!optimizeNested) (it.newValueOpt)?.identifyPolymorphic(protocol.identity)

            wire.send(id, { stream ->
                val versionedFlag = (if (master) 1 else 0) shl versionedFlagShift
                val op = when (it) {
                    is IViewableMap.Event.Add ->    Op.Add
                    is IViewableMap.Event.Update -> Op.Update
                    is IViewableMap.Event.Remove -> Op.Remove
                }
                stream.writeInt(versionedFlag or op.ordinal)

                val version = if (master) ++nextVersion else 0L

                if (master) {
                    pendingForAck.put(it.key, version)
                    stream.writeLong(version)
                }

                keySzr.write(serializationContext, stream, it.key)

                it.newValueOpt?.let { valSzr.write(serializationContext, stream, it) }

                logSend.trace { logmsg(op, version, it.key, it.newValueOpt) }
            })
        }}

        wire.advise(lifetime, id) { stream ->
            val header = stream.readInt()
            val msgVersioned = (header shr versionedFlagShift) != 0
            val op = parseFromOrdinal<Op>(header and ((1 shl versionedFlagShift) - 1))

            val version = if (msgVersioned) stream.readLong() else 0

            val key = keySzr.read(serializationContext, stream)

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

                if (errmsg.isNullOrEmpty())
                    logReceived.trace  { logmsg(Op.Ack, version, key) }
                else
                    logReceived.error {  logmsg(Op.Ack, version, key) + " >> $errmsg"}

            } else {
                val isPut = (op == Op.Add || op == Op.Update)
                val value = if (isPut) valSzr.read(serializationContext, stream) else null

                if (msgVersioned || !master || !pendingForAck.containsKey(key)) {
                    logReceived.trace { logmsg(op, version, key, value) }

                    if (value != null) map[key] = value
                    else map.remove(key)

                } else {
                    logReceived.trace { logmsg(op, version, key, value) + " >> CHANGE IGNORED" }
                }


                if (msgVersioned) {
                    wire.send(id, { innerStream ->
                        innerStream.writeInt((1 shl versionedFlagShift) or Op.Ack.ordinal)
                        innerStream.writeLong(version)
                        keySzr.write(serializationContext, innerStream, key)

                        logSend.trace { logmsg(Op.Ack, version, key) }
                    })

                    if (master) logReceived.error { "Both ends are masters: ${location()}" }
                }

            }

        }

        if (!optimizeNested)
            view(lifetime, { lf, entry -> (entry.value).bindPolymorphic(lf, this, "[${entry.key}]") })
    }



    constructor(keySzr: ISerializer<K> = Polymorphic<K>(), valSzr: ISerializer<V> = Polymorphic<V>()) : this(keySzr, valSzr, ViewableMap())



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
        map.advise(lifetime, {e -> scheduler.invokeOrQueue { handler(e) }})
    }
}
