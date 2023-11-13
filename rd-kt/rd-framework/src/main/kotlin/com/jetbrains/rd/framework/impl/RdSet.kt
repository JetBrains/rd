package com.jetbrains.rd.framework.impl

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.util.collections.SynchronizedSet
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.*
import com.jetbrains.rd.util.string.PrettyPrinter
import com.jetbrains.rd.util.string.print
import com.jetbrains.rd.util.string.printToString
import com.jetbrains.rd.util.trace

@Suppress("UNUSED_PARAMETER")
open class RdSet<T : Any> constructor(val valueSerializer: ISerializer<T>, private val set: ViewableSet<T>)
: RdReactiveBase(), IMutableViewableSet<T> {

    companion object : ISerializer<RdSet<*>> {
        fun<T: Any> read(ctx: SerializationCtx, stream: AbstractBuffer, valueSerializer: ISerializer<T>): RdSet<T> = RdSet(valueSerializer).withId(RdId.read(stream))

        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): RdSet<*> {
            return read(ctx, buffer, Polymorphic())
        }

        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: RdSet<*>) {
            value.rdid.write(buffer)
        }
    }

    override fun deepClone(): IRdBindable = RdSet(valueSerializer).also { for (elem in set) { it.add(elem.deepClonePolymorphic()) } }

    var optimizeNested : Boolean = false

    override fun preInit(lifetime: Lifetime, proto: IProtocol) {
        super.preInit(lifetime, proto)

        proto.wire.advise(lifetime, this)
    }

    override fun init(lifetime: Lifetime, proto: IProtocol, ctx: SerializationCtx) {
        super.init(lifetime, proto, ctx)

        localChange { advise(lifetime) lambda@{ kind, v ->
            if (!isLocalChange) return@lambda

            proto.wire.send(rdid) { buffer ->
                buffer.writeEnum(kind)
                valueSerializer.write(ctx, buffer, v)

                logSend.trace { "set `$location` ($rdid) :: $kind :: ${v.printToString()} "}
            }
        }}
    }

    override fun onWireReceived(proto: IProtocol, buffer: AbstractBuffer, ctx: SerializationCtx, dispatchHelper: IRdWireableDispatchHelper) {
        val kind = buffer.readEnum<AddRemove>()
        val v = valueSerializer.read(ctx, buffer)

        logReceived.trace { "onWireReceived:: $this :: $kind :: ${v.printToString()}" }
        dispatchHelper.dispatch {
            logReceived.trace { "dispatched:: $this :: $kind :: ${v.printToString()}" }
            when (kind) {
                AddRemove.Add -> set.add(v)
                AddRemove.Remove -> set.remove(v)
            }
        }
    }

    constructor(valueSerializer: ISerializer<T> = Polymorphic<T>()) : this(valueSerializer, ViewableSet(SynchronizedSet()))

    override val size: Int
        get() = set.size
    override val change: ISource<IViewableSet.Event<T>>
        get() = set.change

    override fun contains(element: T): Boolean = set.contains(element)

    override fun containsAll(elements: Collection<T>): Boolean = set.containsAll(elements)

    override fun isEmpty(): Boolean = set.isEmpty()

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



    override fun add(element: T): Boolean {
        return localChange { set.add(element) }
    }

    override fun addAll(elements: Collection<T>): Boolean {
        return localChange { set.addAll(elements) }
    }

    override fun iterator(): MutableIterator<T> {
        val delegate = set.iterator()
        return object : MutableIterator<T> by delegate {
            override fun remove() {
                localChange { delegate.remove() }
            }
        }
    }

    override fun remove(element: T): Boolean {
        return localChange { set.remove(element) }
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        return localChange { set.removeAll(elements) }
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        return localChange { set.retainAll(elements) }
    }

    override fun clear() = localChange { set.clear() }

    override fun advise(lifetime: Lifetime, handler: (IViewableSet.Event<T>) -> Unit) {
        if (isBound) assertThreading()
        set.advise(lifetime, handler)
    }


}
