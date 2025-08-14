package com.jetbrains.rd.framework.impl

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.lifetime.isNotAlive
import com.jetbrains.rd.util.reactive.*
import com.jetbrains.rd.util.string.*
import com.jetbrains.rd.util.trace
import java.util.concurrent.atomic.AtomicReference

abstract class RdPropertyBase<T>(val valueSerializer: ISerializer<T>) : RdReactiveBase(), IMutablePropertyBase<T> {
    companion object {
//        @JvmStatic
        fun<T : Any> write0(ctx: SerializationCtx, buffer: AbstractBuffer, prop: RdPropertyBase<T>, value: T?) {
            prop.rdid.notNull().write(buffer)
            if (value != null) {
                buffer.writeBool(true)
                prop.valueSerializer.write(ctx, buffer, value)
            } else {
                buffer.writeBool(false)
            }
        }
    }

    //mastering
    protected var masterVersion = 0

    var defaultValueChanged : Boolean = false
        internal set(value) {
            field = value
        }
    //init
    var optimizeNested: Boolean = false
    private val bindDefinition: AtomicReference<LifetimeDefinition?> = AtomicReference(null)

    protected abstract val property: IMutablePropertyBase<T>

    override val change : ISource<T> get() = property.change

    protected abstract val valueOrNull: T?

    override fun preInit(lifetime: Lifetime, proto: IProtocol) {
        super.preInit(lifetime, proto)

        if (!optimizeNested) {
            val value = valueOrNull
            if (value != null) {
                val definition = tryPreBindValue(lifetime, value, false)
                lifetime.executeIfAlive {
                    val prevDefinition = bindDefinition.getAndSet(definition)
                    assert(prevDefinition?.isNotAlive ?: true)
                }

            }
        }

        proto.wire.advise(lifetime, this)
    }

    override fun init(lifetime: Lifetime, proto: IProtocol, ctx: SerializationCtx) {
        super.init(lifetime, proto, ctx)

        val value = valueOrNull
        var hasInitValue = value != null
        if (hasInitValue && !optimizeNested)
            value.bindPolymorphic()

        advise(lifetime) { v ->
            val shouldIdentify = !hasInitValue
            hasInitValue = false

            if (!isLocalChange)
                return@advise

            if (!optimizeNested && shouldIdentify) {
                // We need to terminate the current lifetime to unbind the existing value before assigning a new value, especially in cases where we are reassigning it.
                bindDefinition.get()?.terminate()

                v.identifyPolymorphic(proto.identity, proto.identity.next(rdid))

                val prevDefinition = bindDefinition.getAndSet(tryPreBindValue(lifetime, v, false))
                prevDefinition?.terminate()
            }

            if (master) masterVersion++

            proto.wire.send(rdid) { buffer ->
                buffer.writeInt(masterVersion)
                valueSerializer.write(ctx, buffer, v)
                logSend.trace { "property `$location` ($rdid):: ver = $masterVersion, value = ${v.printToString()}" }
            }

            if (!optimizeNested && shouldIdentify)
                v.bindPolymorphic()
        }
    }

    override fun unbind() {
        super.unbind()
        bindDefinition.getAndSet(null)
    }

    override fun onWireReceived(proto: IProtocol, buffer: AbstractBuffer, ctx: SerializationCtx, dispatchHelper: IRdWireableDispatchHelper) {
        val version = buffer.readInt()
        val v = valueSerializer.read(ctx, buffer)

        val definition = tryPreBindValue(dispatchHelper.lifetime, v, true)

        logReceived.trace { "onWireReceived:: ${getMessage(version, v)}" }

        dispatchHelper.dispatch {
            val rejected = master && version < masterVersion
            logReceived.trace { "dispatched:: ${getMessage(version, v)}${rejected.condstr { " >> REJECTED" }}" }

            if (rejected) {
                definition?.terminate()
                return@dispatch
            }

            masterVersion = version

            val prevDefinition = bindDefinition.getAndSet(definition)
            prevDefinition?.terminate()
            property.set(v)
        }
    }

    private fun getMessage(version: Int, v: T) =
        "property `$location` ($rdid):: oldver = $masterVersion, newver = $version, value = ${v.printToString()}"

    override fun advise(lifetime: Lifetime, handler: (T) -> Unit) {
        if (isBound) assertThreading()
        property.advise(lifetime, handler)
    }

    private fun tryPreBindValue(lifetime: Lifetime, value: T, bindAlso: Boolean): LifetimeDefinition? {
        if (optimizeNested || value == null)
            return null

        val definition = LifetimeDefinition().apply { id = value }
        try {
            value.preBindPolymorphic(definition.lifetime, this, "$")
            if (bindAlso)
                value.bindPolymorphic()

            lifetime.attach(definition, true)
            return definition
        } catch (e: Throwable) {
            definition.terminate()
            throw e
        }
    }
}



@Suppress("UNCHECKED_CAST")
class RdOptionalProperty<T : Any>(valueSerializer: ISerializer<T> = Polymorphic())
    : RdPropertyBase<T>(valueSerializer), IOptProperty<T> {

    override fun deepClone(): RdOptionalProperty<T> = RdOptionalProperty(valueSerializer).also { if (hasValue) it.set(valueOrThrow.deepClonePolymorphic()) }

    //constructor
    constructor(defaultValue: T, valueSerializer: ISerializer<T> = Polymorphic()) : this(valueSerializer) {
        set(defaultValue)
    }

    //serializers
    companion object : ISerializer<RdOptionalProperty<*>> {

        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): RdOptionalProperty<*> = read(ctx, buffer, Polymorphic())
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: RdOptionalProperty<*>) {
            val value1 = value.valueOrNull
            write0(ctx, buffer, value as RdPropertyBase<Any>, value1)
        }

        fun <T : Any> read(ctx: SerializationCtx, buffer: AbstractBuffer, valueSerializer: ISerializer<T>): RdOptionalProperty<T> {
            val id = RdId.read(buffer)
            val res = RdOptionalProperty(valueSerializer).withId(id)
            if (buffer.readBool()) {
                res.property.set(valueSerializer.read(ctx, buffer))
            }
            return res
        }

    }

    fun slave() : RdOptionalProperty<T> {
        master = false
        return this
    }


    //api
    override val property = OptProperty<T>()
    override val valueOrNull: T?
        get() = property.valueOrNull

    override fun set(newValue: T) {
        localChange {
            defaultValueChanged = true
            property.set(newValue)
        }
    }

    override fun identify(identities: IIdentities, id: RdId) {
        super.identify(identities, id)
        if (!optimizeNested)
            valueOrNull?.identifyPolymorphic(identities, identities.next(id))
    }

    override fun advise(lifetime: Lifetime, handler: (T) -> Unit) {
        super<RdPropertyBase>.advise(lifetime, handler)
    }

    override fun findByRName(rName: RName): RdBindableBase? {
        if (rName == RName.Empty) return this
        val rootName = rName.getNonEmptyRoot()
        val localName = rootName.localName
        if (localName != "$")
            return null

        val value = property.valueOrNull as? RdBindableBase
            ?: return null

        if (rootName == rName)
            return value

        return value.findByRName(rName.dropNonEmptyRoot())
    }

    //pretty printing
    override fun print(printer: PrettyPrinter) {
        super.print(printer)
        printer.print("(ver=$masterVersion) [")
        valueOrNull.let {
            when (it) {
                null -> printer.print(" <not initialized> ")
                else -> printer.indent { it.print(printer) }
            }
            Unit
        }
        printer.print("]")
    }
}

@Suppress("UNCHECKED_CAST")
class RdProperty<T>(defaultValue: T, valueSerializer: ISerializer<T> = Polymorphic())
    : RdPropertyBase<T>(valueSerializer), IProperty<T> {

    override fun deepClone(): RdProperty<T> = RdProperty(value.deepClonePolymorphic(), valueSerializer)

    //serializers
    companion object : ISerializer<RdProperty<*>> {

        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): RdProperty<*> = read(ctx, buffer, Polymorphic<Any?>())
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: RdProperty<*>) =
            write0(ctx, buffer, value as RdPropertyBase<Any>, value.value)

        fun <T> read(ctx: SerializationCtx, buffer: AbstractBuffer, valueSerializer: ISerializer<T>): RdProperty<T> {
            val id = RdId.read(buffer)
            val value = if (buffer.readBool()) valueSerializer.read(ctx, buffer) else null
            return RdProperty(value as T, valueSerializer).withId(id)
        }
    }

    fun slave() : RdProperty<T> {
        master = false
        return this
    }

    override val valueOrNull: T?
        get() = value

    //api
    override val property = Property(defaultValue)

    override var value: T
        get() = property.value
        set(value) = localChange {
            defaultValueChanged = true
            property.value = value
        }

    override fun set(newValue: T) {
        value = newValue
    }

    override fun identify(identities: IIdentities, id: RdId) {
        super.identify(identities, id)
        if (!optimizeNested)
            value?.identifyPolymorphic(identities, identities.next(id))
    }

    override fun advise(lifetime: Lifetime, handler: (T) -> Unit) {
        super<RdPropertyBase>.advise(lifetime, handler)
    }


    override fun findByRName(rName: RName): RdBindableBase? {
        if (rName == RName.Empty) return this
        val rootName = rName.getNonEmptyRoot()
        val localName = rootName.localName
        if (localName != "$")
            return null

        val value = property.value as? RdBindableBase
            ?: return null

        if (rootName == rName)
            return value

        return value.findByRName(rName.dropNonEmptyRoot())
    }

    //pretty printing
    override fun print(printer: PrettyPrinter) {
        super.print(printer)
        printer.print("(ver=$masterVersion) [")
        value.let {
            when (it) {
                null -> printer.print(" <null> ")
                else -> printer.indent { it.print(printer) }
            }
            Unit
        }
        printer.print("]")
    }
}
