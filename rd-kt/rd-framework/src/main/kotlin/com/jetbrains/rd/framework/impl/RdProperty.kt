package com.jetbrains.rd.framework.impl

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.framework.base.bindPolymorphic
import com.jetbrains.rd.framework.base.identifyPolymorphic
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.*
import com.jetbrains.rd.util.string.PrettyPrinter
import com.jetbrains.rd.util.string.condstr
import com.jetbrains.rd.util.string.print
import com.jetbrains.rd.util.string.printToString
import com.jetbrains.rd.util.trace

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

    protected abstract val property: IMutablePropertyBase<T>

    override val change : ISource<T> get() = property.change

    override fun init(lifetime: Lifetime) {
        super.init(lifetime)

        val serializationContext = serializationContext

        if (!optimizeNested)
            change.advise(lifetime) {
                v -> if (isLocalChange) v?.identifyPolymorphic(protocol.identity, protocol.identity.next(rdid))
            }

        advise(lifetime) lambda@{ v ->
            if (!isLocalChange) return@lambda
            if (master) masterVersion++

            wire.send(rdid) { buffer ->
                buffer.writeInt(masterVersion)
                valueSerializer.write(serializationContext, buffer, v)
                logSend.trace { "property `$location` ($rdid):: ver = $masterVersion, value = ${v.printToString()}" }
            }
        }

        wire.advise(lifetime, this)

        if (!optimizeNested)
            view(lifetime) { lf, v -> v?.bindPolymorphic(lf, this, "\$")}
    }

    override fun onWireReceived(buffer: AbstractBuffer) {
        val version = buffer.readInt()
        val v = valueSerializer.read(serializationContext, buffer)

        val rejected = master && version < masterVersion
        logReceived.trace {"property `$location` ($rdid):: oldver = $masterVersion, newver = $version, value = ${v.printToString()}${rejected.condstr { " >> REJECTED" }}"}

        if (rejected) return
        masterVersion = version
        property.set(v)
    }

    override fun advise(lifetime: Lifetime, handler: (T) -> Unit) {
        if (isBound) assertThreading()
        property.advise(lifetime, handler)
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
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: RdOptionalProperty<*>) =
            write0(ctx, buffer, value as RdPropertyBase<Any>, value.valueOrNull)

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
