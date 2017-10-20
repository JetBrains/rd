package com.jetbrains.rider.framework.impl

import com.jetbrains.rider.framework.*
import com.jetbrains.rider.framework.base.*
import com.jetbrains.rider.util.Maybe
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.reactive.IProperty
import com.jetbrains.rider.util.reactive.ISink
import com.jetbrains.rider.util.reactive.Property
import com.jetbrains.rider.util.reactive.hasValue
import com.jetbrains.rider.util.string.PrettyPrinter
import com.jetbrains.rider.util.trace
import java.io.InputStream
import java.io.OutputStream

class RdProperty<T>(val valueSerializer: ISerializer<T>) : RdReactiveBase(), IProperty<T> {

    //constructor
    constructor() : this(Polymorphic<T>())
    constructor(defaultValue: T) : this(Polymorphic<T>()) {
        value = defaultValue
    }

    //serializers
    companion object : ISerializer<RdProperty<*>> {

        override fun read(ctx: SerializationCtx, stream: InputStream): RdProperty<*> = read(ctx, stream, Polymorphic<Any?>())
        override fun write(ctx: SerializationCtx, stream: OutputStream, value: RdProperty<*>) = write0(ctx, stream, value)
//        override val _type : Class<*> get() = throw IllegalStateException("Mustn't be used for polymorphic marshalling")
        
        fun <T> read(ctx: SerializationCtx, stream: InputStream, valueSerializer: ISerializer<T>): RdProperty<T> {
            val id = RdId.read(stream)
            val res = RdProperty(valueSerializer).withId(id)
            if (stream.readBool()) {
                res.property.value = valueSerializer.read(ctx, stream)
            }
            return res
        }

        private fun<T> write0(ctx: SerializationCtx, stream: OutputStream, value: RdProperty<T>) {
            value.id.notNull().write(stream)
            if (value.hasValue /*&& value.value !is IRdBindable*/) {
                stream.writeBool(true)
                value.valueSerializer.write(ctx, stream, value.value)
            } else {
                stream.writeBool(false)
            }
        }
    }


    //mastering
    var isMaster = true
    private var masterVersion = 0

    

    fun slave() : RdProperty<T> {isMaster = false; return this}


    //init
    var optimizeNested: Boolean = false

    override fun identify(ids: IIdentities) {
        super.identify(ids)
        if (!optimizeNested)
            (maybe.asNullable)?.identifyPolymorphic(ids)
    }

    override fun init(lifetime: Lifetime) {
        super.init(lifetime)
        property.name = name

        val serializationContext = serializationContext

        if (!optimizeNested)
            change.advise(lifetime) {
                v -> if (isLocalChange) v?.identifyPolymorphic(protocol.identity)
            }

        advise(lifetime) lambda@{ v ->
            if (!isLocalChange) return@lambda
            if (isMaster) masterVersion++

            wire.send(id) { stream ->
                stream.writeInt(masterVersion)
                valueSerializer.write(serializationContext, stream, v)
                logSend.trace { "property `${location()}` ($id):: ver = $masterVersion, value = ${value.printToString()}" }
            }
        }


        wire.advise(lifetime, id) lambda@{ stream ->
            val version = stream.readInt()
            val v = valueSerializer.read(serializationContext, stream)
            logReceived.trace {"property `${location()}` ($id):: oldver = $masterVersion, newver = $version, value = ${v.printToString()}"}

            if (isMaster && version < masterVersion) return@lambda
            masterVersion = version
            property.value = v
        }

        if (!optimizeNested)
            view(lifetime) { lf, v -> v?.bindPolymorphic(lf, this, "\$")}
    }


    //api
    private val property = Property<T>()
    override val maybe : Maybe<T> get() = property.maybe
    override val change : ISink<T> get() = property.change

    override var value: T
        get() = property.value
        set(value) = localChange {
            property.value = value
        }

    override fun advise(lifetime: Lifetime, handler: (T) -> Unit) {
        if (isBound) assertThreading()
        property.advise(lifetime, handler)
    }

    //pretty printing
    override fun print(printer: PrettyPrinter) {
        super.print(printer)
        printer.print("(ver=$masterVersion) [")
        maybe.let {
            when (it) {
                is Maybe.None -> printer.print(" <not initialized> ")
                is Maybe.Just -> printer.indent { it.value.print(printer) }
            }
            Unit
        }
        printer.print("]")
    }
}