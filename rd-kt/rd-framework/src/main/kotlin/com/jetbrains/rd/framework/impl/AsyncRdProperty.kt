package com.jetbrains.rd.framework.impl

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.util.Maybe
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.isNotAlive
import com.jetbrains.rd.util.reactive.*
import com.jetbrains.rd.util.string.*
import com.jetbrains.rd.util.threadLocalWithInitial
import com.jetbrains.rd.util.threading.asSequentialScheduler
import com.jetbrains.rd.util.trace

interface IAsyncSource2<T> {
    fun adviseOn(lifetime : Lifetime, scheduler : IScheduler, handler : (T) -> Unit)
}

interface IAsyncProperty<T> : IAsyncSource2<T> {
    val maybe: Maybe<T>
    val value: T
        get() = maybe.orElseThrow { IllegalStateException("Property is not initialized") }
}

interface IMutableAsyncProperty<T> : IAsyncProperty<T> {
    override var value: T
        get() = super.value
        set(value) = set(value)

    fun set(value: T)
}

class AsyncRdProperty<T>(val valueSerializer: ISerializer<T> = Polymorphic()) : IMutableAsyncProperty<T>, IRdBindable, IPrintable, IRdWireable {

    companion object : ISerializer<AsyncRdProperty<*>> {

        fun <T : Any> write0(ctx: SerializationCtx, buffer: AbstractBuffer, prop: AsyncRdProperty<T>, maybe: Maybe<T?>) {
            prop.rdid.notNull().write(buffer)
            if (maybe.hasValue) {
                buffer.writeBool(true)
                prop.valueSerializer.write(ctx, buffer, maybe.asNullable as T)
            } else {
                buffer.writeBool(false)
            }
        }


        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): AsyncRdProperty<*> = read(ctx, buffer, Polymorphic())
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, property: AsyncRdProperty<*>) {
            synchronized(property.property) {
                write0(ctx, buffer, property as AsyncRdProperty<Any>, property.maybe)
            }
        }

        fun <T : Any> read(ctx: SerializationCtx, buffer: AbstractBuffer, valueSerializer: ISerializer<T>): AsyncRdProperty<T> {
            val id = RdId.read(buffer)
            val res = AsyncRdProperty(valueSerializer).withId(id)
            if (buffer.readBool()) {
                res.property.set(Maybe.Just(valueSerializer.read(ctx, buffer)))
            }
            return res
        }
    }

    private val isLocalChange = threadLocalWithInitial { false }
    private val property = Property<Maybe<T>>(Maybe.None)

    constructor(defaultValue: T, valueSerializer: ISerializer<T> = Polymorphic()) : this(valueSerializer) {
        property.value = Maybe.Just(defaultValue)
    }


    fun withId(id: RdId) : AsyncRdProperty<T> {
        require(this.rdid == RdId.Null) { "this.id != RdId.Null, but ${this.rdid}" }
        require(id != RdId.Null) {"id != RdId.Null"}

        return this.apply { this.rdid = id }
    }


    //mastering
    protected var masterVersion = 0

    //init
    var optimizeNested: Boolean
        get() = true
        set(value) {}

    var async: Boolean
        get() = true
        set(value) { }

    private var masterOverridden : Boolean? = null

    var master : Boolean
        get() = masterOverridden ?: protocol?.isMaster ?: false
        set(value) { masterOverridden = value }

    val change : IAsyncSource2<T> get() = object : IAsyncSource2<T> {
        override fun adviseOn(lifetime: Lifetime, scheduler: IScheduler, handler: (T) -> Unit) {
            val scheduler = scheduler.asSequentialScheduler()
            property.change.advise(lifetime) {
                if (it.hasValue)
                    scheduler.queue { handler(it.asNullable as T) }
            }
        }
    }

    override var rdid: RdId = RdId.Null
        internal set

    override var location: RName = RName("<<not bound>>")
        private set

    var parent: IRdDynamic? = null
        protected set

    private var bindLifetime: Lifetime = Lifetime.Terminated
        private set

    override fun preBind(lifetime: Lifetime, parent: IRdDynamic, name: String) {
        val proto = parent.protocol ?: return

        synchronized(property) {
            lifetime.bracketIfAlive({
                this.parent = parent
                location = parent.location.sub(name, ".")
                bindLifetime = lifetime
            }, {
                synchronized(property) {
                    location = location.sub("<<unbound>>", "::")
                    rdid = RdId.Null
                    this.parent = null
                }
            }) ?: return
        }

        proto.wire.advise(lifetime, this)
    }

    override fun bind() {
        synchronized(property) {
            val proto = protocol ?: return
            val ctx = serializationContext ?: return

            val lifetime = bindLifetime
            if (lifetime.isNotAlive)
                return

            property.advise(lifetime) { v ->
                if (!isLocalChange.get())
                    return@advise

                if (master) masterVersion++

                proto.wire.send(rdid) { buffer ->
                    buffer.writeInt(masterVersion)
                    valueSerializer.write(ctx, buffer, v.asNullable as T)
                    RdReactiveBase.logSend.trace { "property `$location` ($rdid):: ver = $masterVersion, value = ${v.printToString()}" }
                }
            }

        }
    }

    override fun identify(identities: IIdentities, id: RdId) {
        require(!id.isNull) { "Assigned RdId mustn't be null, entity: $this" }

        synchronized (property) {
            require(rdid.isNull)  { "Already has RdId: ${rdid}, entity: $this" }
            rdid = id
        }
    }

    override fun onWireReceived(buffer: AbstractBuffer, dispatchHelper: IRdWireableDispatchHelper) {
        val ctx = serializationContext ?: return

        val version = buffer.readInt()
        val v = valueSerializer.read(ctx, buffer)

        synchronized(property) {
            val rejected = master && version < masterVersion
            RdReactiveBase.logReceived.trace { "property `$location` ($rdid):: oldver = $masterVersion, newver = $version, value = ${v.printToString()}${rejected.condstr { " >> REJECTED" }}" }

            if (rejected)
                return

            masterVersion = version
            property.set(Maybe.Just(v))
        }
    }

    override val protocol: IProtocol? get() = parent?.protocol
    override val serializationContext: SerializationCtx? get() = parent?.serializationContext

    override fun adviseOn(lifetime: Lifetime, scheduler: IScheduler, handler: (T) -> Unit) {
        val scheduler = scheduler.asSequentialScheduler()
        synchronized(property) {
            property.advise(lifetime) {
                if (it.hasValue)
                    scheduler.queue { handler(it.asNullable as T) }
            }
        }
    }

    override fun deepClone(): AsyncRdProperty<T> {
        synchronized(property) {
            val maybe = property.value
            return AsyncRdProperty(valueSerializer).also { if (maybe.hasValue) it.set(((maybe.asNullable as T).deepClonePolymorphic())) }
        }
    }

    //pretty printing
    override fun print(printer: PrettyPrinter) {
        super.print(printer)
        synchronized(property) {
            printer.print("(ver=$masterVersion) [")
            val maybe = property.value
            maybe.asNullable.let {
                when (it) {
                    null -> printer.print(" <not initialized> ")
                    else -> printer.indent { it.print(printer) }
                }
                Unit
            }
            printer.print("]")
        }
    }

    fun slave() : AsyncRdProperty<T> {
        master = false
        return this
    }

    override fun set(value: T) {
        isLocalChange.set(true)
        try {
            synchronized(property) {
                property.value = Maybe.Just(value)
            }
        } finally {
            isLocalChange.set(false)
        }
    }

    override val maybe: Maybe<T>
        get() = synchronized(property) {
            property.value
        }
}
