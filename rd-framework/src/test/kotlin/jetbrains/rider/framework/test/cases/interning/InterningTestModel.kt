package com.jetbrains.rider.framework.test.cases.interning

import com.jetbrains.rider.framework.*
import com.jetbrains.rider.framework.base.IPrintable
import com.jetbrains.rider.framework.base.RdBindableBase
import com.jetbrains.rider.framework.base.print
import com.jetbrains.rider.framework.impl.RdMap
import com.jetbrains.rider.framework.impl.RdProperty
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.reactive.IMutableViewableMap
import com.jetbrains.rider.util.string.PrettyPrinter
import java.io.InputStream
import java.io.OutputStream

data class WrappedStringModel(
    val text : String
) : IPrintable {
    //companion

    companion object : IMarshaller<WrappedStringModel> {
        override val _type: Class<WrappedStringModel> = WrappedStringModel::class.java

        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, stream: InputStream): WrappedStringModel {
            val text = ctx.readInterned(stream, { ctx, stream -> stream.readString() })
            return WrappedStringModel(text)
        }

        override fun write(ctx: SerializationCtx, stream: OutputStream, value: WrappedStringModel) {
            ctx.writeInterned(stream, value.text, { ctx, stream, value -> stream.writeString(value) })
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
        override fun read(ctx: SerializationCtx, stream: InputStream): InterningTestModel {
            val ctx = ctx.withInternRootHere(false)
            val searchLabel = stream.readString()
            val _issues = RdMap.read(ctx, stream, FrameworkMarshallers.Int, WrappedStringModel)
            return InterningTestModel(searchLabel, _issues).apply { mySerializationContext = ctx }
        }

        override fun write(ctx: SerializationCtx, stream: OutputStream, value: InterningTestModel) {
            val ctx = ctx.withInternRootHere(true)
            value.mySerializationContext = ctx
            stream.writeString(value.searchLabel)
            RdMap.write(ctx, stream, value._issues)
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
    override fun identify(ids: IIdentities) {
        _issues.identify(ids)
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
        override fun read(ctx: SerializationCtx, stream: InputStream): InterningNestedTestModel {
            val value = stream.readString()
            val inner = stream.readNullable { ctx.readInterned(stream, this::read) }
            return InterningNestedTestModel(value, inner)
        }

        override fun write(ctx: SerializationCtx, stream: OutputStream, value: InterningNestedTestModel) {
            stream.writeString(value.value)
            stream.writeNullable(value.inner, { ctx.writeInterned(stream, it, this::write) })
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

class PropertyHolderWithInternRoot<T>(val property: RdProperty<T>, private val mySerializationContext: SerializationCtx) : RdBindableBase() {
    override fun init(lifetime: Lifetime) {
        property.bind(lifetime, this, "propertyHolderWithInternRoot")
    }

    override fun identify(ids: IIdentities) {
        property.identify(ids)
    }



    override val serializationContext: SerializationCtx
        get() = mySerializationContext
}