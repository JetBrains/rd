package com.jetbrains.rider.framework.test.cases.interning

import com.jetbrains.rider.framework.*
import com.jetbrains.rider.framework.base.AbstractBuffer
import com.jetbrains.rider.util.string.IPrintable
import com.jetbrains.rider.framework.base.RdBindableBase
import com.jetbrains.rider.util.string.print
import com.jetbrains.rider.framework.impl.RdMap
import com.jetbrains.rider.framework.impl.RdOptionalProperty
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.reactive.IMutableViewableMap
import com.jetbrains.rider.util.string.PrettyPrinter

data class WrappedStringModel(
    val text : String
) : IPrintable {
    //companion

    companion object : IMarshaller<WrappedStringModel> {
        override val _type: Class<WrappedStringModel> = WrappedStringModel::class.java

        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): WrappedStringModel {
            val text = ctx.readInterned(buffer, { ctx, stream -> stream.readString() })
            return WrappedStringModel(text)
        }

        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: WrappedStringModel) {
            ctx.writeInterned(buffer, value.text, { ctx, stream, value -> stream.writeString(value) })
        }

    }
    //fields
    //initializer
    //secondary constructor
    //init method
    //identify method
    //equals trait
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as WrappedStringModel

        if (text != other.text) return false

        return true
    }
    //hash code trait
    override fun hashCode(): Int {
        var __r = 0
        __r = __r*31 + text.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter) {
        printer.println("WrappedStringModel (")
        printer.indent {
            print("text = "); text.print(printer); println()
        }
        printer.print(")")
    }
}


class InterningTestModel(
    val searchLabel : String,
    private val _issues : RdMap<Int, WrappedStringModel>
    ) : RdBindableBase() {
    //companion

    companion object : IMarshaller<InterningTestModel> {
        override val _type: Class<InterningTestModel> = InterningTestModel::class.java

        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): InterningTestModel {
            val ctx = ctx.withInternRootHere(false)
            val searchLabel = buffer.readString()
            val _issues = RdMap.read(ctx, buffer, FrameworkMarshallers.Int, WrappedStringModel)
            return InterningTestModel(searchLabel, _issues).apply { mySerializationContext = ctx }
        }

        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: InterningTestModel) {
            val ctx = ctx.withInternRootHere(true)
            value.mySerializationContext = ctx
            buffer.writeString(value.searchLabel)
            RdMap.write(ctx, buffer, value._issues)
        }

    }
    //fields
    val issues : IMutableViewableMap<Int, WrappedStringModel> get() = _issues

    private var mySerializationContext : SerializationCtx? = null
    override val serializationContext: SerializationCtx
        get() = mySerializationContext ?: throw IllegalStateException("Serialization context was accessed too soon for $name")

    //initializer
    init {
        _issues.optimizeNested = true
    }

    //secondary constructor
    constructor(
        searchLabel : String
    ) : this (
        searchLabel,
        RdMap<Int, WrappedStringModel>(FrameworkMarshallers.Int, WrappedStringModel)
    )

    //init method
    override fun init(lifetime: Lifetime) {
        _issues.bind(lifetime, this, "issues")
    }
    //identify method
    override fun identify(identities: IIdentities, ids: RdId) {
        _issues.identify(identities, ids.mix("issues"))
    }
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter) {
        printer.println("InterningTestModel (")
        printer.indent {
            print("searchLabel = "); searchLabel.print(printer); println()
            print("issues = "); _issues.print(printer); println()
        }
        printer.print(")")
    }
}

class InterningNestedTestModel(val value: String, val inner: InterningNestedTestModel?) {
    companion object : IMarshaller<InterningNestedTestModel> {
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): InterningNestedTestModel {
            val value = buffer.readString()
            val inner = buffer.readNullable { ctx.readInterned(buffer, this::read) }
            return InterningNestedTestModel(value, inner)
        }

        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: InterningNestedTestModel) {
            buffer.writeString(value.value)
            buffer.writeNullable(value.inner, { ctx.writeInterned(buffer, it, this::write) })
        }

        override val _type: Class<*> = InterningNestedTestModel::class.java
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InterningNestedTestModel

        if (value != other.value) return false
        if (inner != other.inner) return false

        return true
    }

    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + (inner?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "InterningNestedTestModel(value='$value', inner=$inner)"
    }
}

class PropertyHolderWithInternRoot<T : Any>(val property: RdOptionalProperty<T>, private val mySerializationContext: SerializationCtx) : RdBindableBase() {
    override fun init(lifetime: Lifetime) {
        property.bind(lifetime, this, "propertyHolderWithInternRoot")
    }

    override fun identify(identities: IIdentities, ids: RdId) {
        property.identify(identities, ids.mix("propertyHolderWithInternRoot"))
    }



    override val serializationContext: SerializationCtx
        get() = mySerializationContext
}