package com.jetbrains.rd.framework.impl

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.util.collections.SynchronizedList
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.lifetime.isAlive
import com.jetbrains.rd.util.parseFromOrdinal
import com.jetbrains.rd.util.reactive.IMutableViewableList
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.reactive.IViewableList
import com.jetbrains.rd.util.reactive.ViewableList
import com.jetbrains.rd.util.string.*
import com.jetbrains.rd.util.trace


@Suppress("UNUSED_PARAMETER")
class RdList<V : Any> private constructor(val valSzr: ISerializer<V>, private val list: ViewableList<V>, private var nextVersion: Long = 1L)
    : RdReactiveBase(), IMutableViewableList<V> by list {

    companion object : ISerializer<RdList<*>> {
        private enum class Op {Add, Update, Remove} // update versionedFlagShift when changing

        fun<V:Any> read(ctx: SerializationCtx, buffer: AbstractBuffer, valSzr: ISerializer<V>): RdList<V> {
            return RdList(valSzr, ViewableList(), buffer.readLong()).withId(RdId.read(buffer))
        }

        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): RdList<*> {
            return read(ctx, buffer, Polymorphic())
        }

        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: RdList<*>) : Unit = value.run {
            buffer.writeLong(nextVersion)
            rdid.write(buffer)
        }

        const val versionedFlagShift = 2 // update when changing Op
    }

    override fun deepClone(): IRdBindable = RdList(valSzr).also { for (elem in list) { it.add(elem.deepClonePolymorphic()) } }

    var optimizeNested: Boolean = false
    private var bindDefinitions: MutableList<LifetimeDefinition?>? = null


    private fun logmsg(op: Op, version: Long, key: Int, value: V? = null) : String {
        return "list `$location` ($rdid) :: ${op.name} :: key = ${key.printToString()}"+
            (version > 0).condstr   { " :: version = $version" }  +
            (value != null).condstr { " :: value = ${value.printToString()}" }
    }

    override fun unbind() {
        super.unbind()
        bindDefinitions = null
    }

    override fun preInit(lifetime: Lifetime, proto: IProtocol) {
        super.preInit(lifetime, proto)

        if (!optimizeNested) {

            val definitions = SynchronizedList<LifetimeDefinition?>()

            for (index in 0 until size) {
                val item = this[index]
                if (item != null) {
                    item.identifyPolymorphic(proto.identity, proto.identity.next(rdid))
                    definitions.add(tryPreBindValue(lifetime, item, index, false))
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

                    if (it !is IViewableList.Event.Add)
                        definitions[it.index]?.terminate()

                    if (it is IViewableList.Event.Remove)
                        definitions.removeAt(it.index)

                    val value = it.newValueOpt
                    if (it !is IViewableList.Event.Remove) {
                        value.identifyPolymorphic(proto.identity, proto.identity.next(rdid))
                        definitions.add(it.index, tryPreBindValue(lifetime, value, it.index, false))
                    }
                }
            }
        }

        localChange { advise(lifetime) lambda@{
            if (!isLocalChange) return@lambda

            proto.wire.send(rdid) { buffer ->
                val op = when (it) {
                    is IViewableList.Event.Add ->    Op.Add
                    is IViewableList.Event.Update -> Op.Update
                    is IViewableList.Event.Remove -> Op.Remove
                }
                buffer.writeLong(op.ordinal.toLong() or (nextVersion++ shl versionedFlagShift))
                buffer.writeInt(it.index)

                it.newValueOpt?.let { valSzr.write(ctx, buffer, it) }

                logSend.trace { logmsg(op, nextVersion-1, it.index, it.newValueOpt) }
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
        
        val stringIndex = localName.removeSurrounding("[", "]")
        val index = stringIndex.toIntOrNull() 
            ?: return null

        val element = list.getOrNull(index) as? RdBindableBase
            ?: return null
        
        if (rootName == rName)
            return element
        
        return element.findByRName(rName.dropNonEmptyRoot())
    }

    override fun onWireReceived(proto: IProtocol, buffer: AbstractBuffer, ctx: SerializationCtx, dispatchHelper: IRdWireableDispatchHelper) {
        val header = buffer.readLong()
        val version = header shr versionedFlagShift
        val op = parseFromOrdinal<Op>((header and ((1 shl versionedFlagShift) - 1L)).toInt())
        val index = buffer.readInt()


        val value = if ((op == Op.Add || op == Op.Update)) valSzr.read(ctx, buffer) else null

        logReceived.trace { logmsg(op, version, index, value) }

        val lifetime = dispatchHelper.lifetime
        val definition = tryPreBindValue(lifetime, value, index, true)

        dispatchHelper.dispatch {
            if (version != nextVersion) {
                definition?.terminate()
                error("Version conflict for $location}. Expected version $nextVersion, received $version. Are you modifying a list from two sides?")
            }

            nextVersion++

            val definitions = tryGetBindDefinitions(lifetime)
            when (op) { // todo: better conflict resolution
                RdList.Companion.Op.Add -> {
                    if (index < 0) {
                        definitions?.add(definition)
                        list.add(value!!)
                    } else {
                        definitions?.add(index, definition)
                        list.add(index, value!!)
                    }
                }

                RdList.Companion.Op.Update -> {
                    if (definitions != null) {
                        definitions[index]?.terminate()
                        definitions[index] = definition
                    }
                    list[index] = value!!
                }

                RdList.Companion.Op.Remove -> {
                    definitions?.removeAt(index)?.terminate()
                    list.removeAt(index)
                }
            }
        }
    }

    private fun tryGetBindDefinitions(lifetime: Lifetime): MutableList<LifetimeDefinition?>?  {
        val definitions = bindDefinitions
        return if (lifetime.isAlive) definitions else null
    }

    private fun tryPreBindValue(lifetime: Lifetime, value: V?, index: Int, bindAlso: Boolean): LifetimeDefinition? {
        if (optimizeNested || value == null)
            return null

        val definition = LifetimeDefinition().apply { id = value }
        try {
            value.preBindPolymorphic(definition.lifetime, this, "[$index]")
            if (bindAlso)
                value.bindPolymorphic()

            (lifetime as LifetimeDefinition).attach(definition, true)

            return definition
        } catch (e: Throwable) {
            definition.terminate()
            throw e
        }
    }

    constructor(valSzr: ISerializer<V> = Polymorphic<V>()) : this(valSzr, ViewableList(SynchronizedList()))



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
