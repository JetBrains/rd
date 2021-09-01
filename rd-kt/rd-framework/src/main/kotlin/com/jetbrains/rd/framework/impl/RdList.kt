package com.jetbrains.rd.framework.impl

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.parseFromOrdinal
import com.jetbrains.rd.util.reactive.IMutableViewableList
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.reactive.IViewableList
import com.jetbrains.rd.util.reactive.ViewableList
import com.jetbrains.rd.util.string.PrettyPrinter
import com.jetbrains.rd.util.string.condstr
import com.jetbrains.rd.util.string.print
import com.jetbrains.rd.util.string.printToString
import com.jetbrains.rd.util.trace


@Suppress("UNUSED_PARAMETER")
class RdList<V : Any> private constructor(val valSzr: ISerializer<V>, private val list: ViewableList<V>, private var nextVersion: Long = 1L)
    : RdReactiveBase(), IMutableViewableList<V> by list {

    companion object {
        private enum class Op {Add, Update, Remove} // update versionedFlagShift when changing

        fun<V:Any> read(ctx: SerializationCtx, buffer: AbstractBuffer, valSzr: ISerializer<V>): RdList<V> {
            return RdList(valSzr, ViewableList(), buffer.readLong()).withId(RdId.read(buffer))
        }

        fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: RdList<*>) : Unit = value.run {
            buffer.writeLong(nextVersion)
            rdid.write(buffer)
        }

        const val versionedFlagShift = 2 // update when changing Op
    }

    override fun deepClone(): IRdBindable = RdList(valSzr).also { for (elem in list) { it.add(elem.deepClonePolymorphic()) } }

    var optimizeNested: Boolean = false


    private fun logmsg(op: Op, version: Long, key: Int, value: V? = null) : String {
        return "list `$location` ($rdid) :: ${op.name} :: key = ${key.printToString()}"+
            (version > 0).condstr   { " :: version = $version" }  +
            (value != null).condstr { " :: value = ${value.printToString()}" }
    }

    override fun init(lifetime: Lifetime) {
        super.init(lifetime)

        localChange { advise(lifetime) lambda@{
            if (!isLocalChange) return@lambda

            if (!optimizeNested) (it.newValueOpt)?.identifyPolymorphic(protocol.identity, protocol.identity.next(rdid))

            wire.send(rdid) { buffer ->
                val op = when (it) {
                    is IViewableList.Event.Add ->    Op.Add
                    is IViewableList.Event.Update -> Op.Update
                    is IViewableList.Event.Remove -> Op.Remove
                }
                buffer.writeLong(op.ordinal.toLong() or (nextVersion++ shl versionedFlagShift))
                buffer.writeInt(it.index)

                it.newValueOpt?.let { valSzr.write(serializationContext, buffer, it) }

                logSend.trace { logmsg(op, nextVersion-1, it.index, it.newValueOpt) }
            }
        }}

        wire.advise(lifetime, this)

        if (!optimizeNested)
            view(lifetime) { lf, index, value -> value.bindPolymorphic(lf, this, "[$index]") }
    }


    override fun onWireReceived(buffer: AbstractBuffer) {
        val header = buffer.readLong()
        val version = header shr versionedFlagShift
        val op = parseFromOrdinal<Op>((header and ((1 shl versionedFlagShift) - 1L)).toInt())
        val index = buffer.readInt()


        val value = if ((op == Op.Add || op == Op.Update)) valSzr.read(serializationContext, buffer) else null

        logReceived.trace { logmsg(op, version, index, value) }

        require(version == nextVersion) {
            "Version conflict for $location}. Expected version $nextVersion, received $version. Are you modifying a list from two sides?"
        }

        nextVersion++

        when(op) { // todo: better conflict resolution
            RdList.Companion.Op.Add -> if (index < 0) list.add(value!!) else list.add(index, value!!)
            RdList.Companion.Op.Update -> list[index] = value!!
            RdList.Companion.Op.Remove -> list.removeAt(index)
        }
    }

    constructor(valSzr: ISerializer<V> = Polymorphic<V>()) : this(valSzr, ViewableList())



    override fun print(printer: PrettyPrinter) {
        super.print(printer)
        printer.print(" [")
        if (!isEmpty()) printer.println()

        printer.indent {
            forEach {
                it.print(printer)
                printer.println()
            }
        }
        printer.print("]")
    }

    override fun iterator() = listIterator(0)
    override fun add(element: V) = localChange { list.add(element) }
    override fun add(index: Int, element: V) = localChange { list.add(index, element) }
    override fun addAll(index: Int, elements: Collection<V>): Boolean = localChange { list.addAll(index, elements) }
    override fun addAll(elements: Collection<V>): Boolean = localChange { list.addAll(elements) }
    override fun listIterator() = listIterator(0)
    override fun listIterator(index: Int): MutableListIterator<V> {
        val iterator = list.listIterator(index)
        return object : MutableListIterator<V> by iterator {
            override fun add(element: V) = localChange { iterator.add(element) }
            override fun remove() = localChange { iterator.remove() }
            override fun set(element: V) = localChange { iterator.set(element) }
        }
    }
    override fun remove(element: V): Boolean = localChange { list.remove(element) }
    override fun removeAll(elements: Collection<V>): Boolean = localChange { list.removeAll(elements) }
    override fun removeAt(index: Int): V = localChange { list.removeAt(index) }
    override fun retainAll(elements: Collection<V>): Boolean = localChange { list.retainAll(elements) }
    override fun set(index: Int, element: V): V = localChange { list.set(index, element) }
    override fun clear() = localChange { list.clear() }


    override fun advise(lifetime: Lifetime, handler: (IViewableList.Event<V>) -> Unit) {
        if (isBound) assertThreading()
        list.advise(lifetime, handler)
    }

    fun adviseOn(lifetime: Lifetime, scheduler: IScheduler, handler: (IViewableList.Event<V>) -> Unit) {
        if (isBound) assertThreading()
        list.advise(lifetime) { e -> scheduler.invokeOrQueue { handler(e) }}
    }


}
