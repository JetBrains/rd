package com.jetbrains.rd.framework.impl

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.reactive.IViewableSet
import com.jetbrains.rd.util.string.RName
import com.jetbrains.rd.util.threading.SynchronousScheduler
import java.util.function.*

class AsyncRdSet<T : Any> private constructor(
    private val set: BackendRdSet<T>
) : IRdBindable,  MutableSet<T>, IAsyncSource2<IViewableSet.Event<T>>{

    constructor(valueSerializer: ISerializer<T> = Polymorphic()) : this(BackendRdSet(valueSerializer))

    companion object : ISerializer<AsyncRdSet<*>> {
        fun <T : Any> read(ctx: SerializationCtx, buffer: AbstractBuffer, valueSerializer: ISerializer<T> = Polymorphic()): AsyncRdSet<T> = AsyncRdSet(valueSerializer).withId(RdId.read(buffer))
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: AsyncRdSet<*>) = value.rdid.write(buffer)

        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): AsyncRdSet<*> {
            return read(ctx, buffer, Polymorphic())
        }
    }

    val change: IAsyncSource2<IViewableSet.Event<T>> = object : IAsyncSource2<IViewableSet.Event<T>> {
        override fun adviseOn(lifetime: Lifetime, scheduler: IScheduler, handler: (IViewableSet.Event<T>) -> Unit) {
            set.change.advise(lifetime) {
                scheduler.queue { handler(it) }
            }
        }
    }

    override fun adviseOn(lifetime: Lifetime, scheduler: IScheduler, handler: (IViewableSet.Event<T>) -> Unit) {
        synchronized(set) {
            set.advise(lifetime) { event ->
                scheduler.queue { handler(event) }
            }
        }
    }

    fun withId(id: RdId) : AsyncRdSet<T> {
        synchronized(set) {
            set.withId(id)
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
        get() = synchronized(set) {
            set.rdid
        }

    override fun preBind(lf: Lifetime, parent: IRdDynamic, name: String) {
        synchronized(set) {
            set.preBind(lf, parent, name)
        }
    }

    override fun bind() {
        synchronized(set) {
            set.bind()
        }
    }

    override fun identify(identities: IIdentities, id: RdId) {
        synchronized(set) {
            set.identify(identities, id)
        }
    }

    override fun deepClone(): IRdBindable {
        synchronized(set) {
            val clone = set.deepClone()
            return AsyncRdSet(clone)
        }
    }

    override val protocol: IProtocol?
        get() = set.protocol

    override val serializationContext: SerializationCtx?
        get() = set.serializationContext

    override val location: RName
        get() = set.location

    override val size: Int
        get() = set.size

    override fun add(element: T): Boolean {
        return synchronized(set) {
            set.add(element)
        }
    }

    override fun addAll(elements: Collection<T>): Boolean {
        return synchronized(set) {
            set.addAll(elements)
        }
    }

    override fun clear() {
        synchronized(set) {
            set.clear()
        }
    }

    override fun iterator(): MutableIterator<T> {
       val iterator =  set.iterator()

        return object : MutableIterator<T> {
            override fun hasNext(): Boolean = iterator.hasNext()
            override fun next(): T = iterator.next()

            override fun remove() {
                synchronized(set) {
                    iterator.remove()
                }
            }
        }
    }

    override fun remove(element: T): Boolean {
        return synchronized(set) {
            set.remove(element)
        }
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        return synchronized(set) {
            set.removeAll(elements)
        }
    }

    override fun removeIf(filter: Predicate<in T>): Boolean {
        TODO()
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        return synchronized(set) {
            set.retainAll(elements)
        }
    }

    override fun contains(element: T): Boolean {
        return set.contains(element)
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        return set.containsAll(elements)
    }

    override fun isEmpty(): Boolean {
        return set.isEmpty()
    }

    private class BackendRdSet<T : Any>(
        valueSerializer: ISerializer<T>
    ) : RdSet<T>(valueSerializer) {

        init {
            async = true
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

        override fun deepClone(): BackendRdSet<T> = BackendRdSet(valueSerializer).also { for (elem in it) { it.add(elem.deepClonePolymorphic()) } }
    }
}