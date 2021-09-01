package com.jetbrains.rd.framework.impl

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.AddRemove
import com.jetbrains.rd.util.reactive.IMutableViewableSet
import com.jetbrains.rd.util.reactive.IViewableSet
import com.jetbrains.rd.util.reactive.ViewableSet
import com.jetbrains.rd.util.string.PrettyPrinter
import com.jetbrains.rd.util.string.print
import com.jetbrains.rd.util.string.printToString
import com.jetbrains.rd.util.trace


@Suppress("UNUSED_PARAMETER")
class RdSet<T : Any> constructor(val valueSerializer: ISerializer<T>, private val set: ViewableSet<T>)
: RdReactiveBase(), IMutableViewableSet<T> by set {

    companion object {
        fun<T: Any> read(ctx: SerializationCtx, stream: AbstractBuffer, valueSerializer: ISerializer<T>): RdSet<T> = RdSet(valueSerializer).withId(RdId.read(stream))
        fun<T: Any> write(ctx: SerializationCtx, stream: AbstractBuffer, value: RdSet<T>) = value.rdid.write(stream)
    }

    override fun deepClone(): IRdBindable = RdSet(valueSerializer).also { for (elem in set) { it.add(elem.deepClonePolymorphic()) } }

    var optimizeNested : Boolean = false

    override fun init(lifetime: Lifetime) {
        super.init(lifetime)

        val serializationContext = serializationContext

        localChange { advise(lifetime) lambda@{ kind, v ->
            if (!isLocalChange) return@lambda

            wire.send(rdid) { buffer ->
                buffer.writeEnum(kind)
                valueSerializer.write(serializationContext, buffer, v)

                logSend.trace { "set `$location` ($rdid) :: $kind :: ${v.printToString()} "}
            }
        }}

        wire.advise(lifetime, this)
    }

    override fun onWireReceived(buffer: AbstractBuffer) {
        val kind = buffer.readEnum<AddRemove>()
        val v = valueSerializer.read(serializationContext, buffer)

        //todo maybe identify is forgotten

        when (kind) {
            AddRemove.Add -> set.add(v)
            AddRemove.Remove -> set.remove(v)
        }
    }

    constructor(valueSerializer: ISerializer<T> = Polymorphic<T>()) : this(valueSerializer, ViewableSet())



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
