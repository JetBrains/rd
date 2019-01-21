@file:Suppress("PackageDirectoryMismatch", "UnusedImport", "unused", "LocalVariableName")
package com.jetbrains.rd.framework.test.cases.interning

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.framework.impl.*

import com.jetbrains.rd.util.lifetime.*
import com.jetbrains.rd.util.reactive.*
import com.jetbrains.rd.util.string.*
import com.jetbrains.rd.util.trace
import com.jetbrains.rd.util.Date
import com.jetbrains.rd.util.UUID
import com.jetbrains.rd.util.URI
import kotlin.reflect.KClass



class InterningRoot1 private constructor(
) : RdExtBase() {
    //companion

    companion object : ISerializersOwner {

        override fun registerSerializersCore(serializers: ISerializers) {
            serializers.register(InterningTestModel)
            serializers.register(InterningNestedTestModel)
            serializers.register(InterningNestedTestStringModel)
            serializers.register(InterningProtocolLevelModel)
            serializers.register(InterningMtModel)
            serializers.register(InterningExtensionHolder)
            serializers.register(WrappedStringModel)
            serializers.register(ProtocolWrappedStringModel)
            InterningRoot1.register(serializers)
            InterningExt.register(serializers)
        }


        fun create(lifetime: Lifetime, protocol: IProtocol): InterningRoot1 {
            InterningRoot1.register(protocol.serializers)

            return InterningRoot1().apply {
                identify(protocol.identity, RdId.Null.mix("InterningRoot1"))
                bind(lifetime, protocol, "InterningRoot1")
            }
        }


        const val serializationHash = 2016272947314984652L
    }
    override val serializersOwner: ISerializersOwner get() = InterningRoot1
    override val serializationHash: Long get() = InterningRoot1.serializationHash

    //fields
    //initializer
    //secondary constructor
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter) {
        printer.println("InterningRoot1 (")
        printer.print(")")
    }
}


class InterningExtensionHolder (
) : RdBindableBase() {
    //companion

    companion object : IMarshaller<InterningExtensionHolder> {
        override val _type: KClass<InterningExtensionHolder> = InterningExtensionHolder::class

        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): InterningExtensionHolder {
            val _id = RdId.read(buffer)
            return InterningExtensionHolder().withId(_id).apply { mySerializationContext = ctx.withInternRootsHere(this, "OutOfExt") }
        }

        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: InterningExtensionHolder) {
            value.rdid.write(buffer)
            value.mySerializationContext = ctx.withInternRootsHere(value, "OutOfExt")
        }

    }
    //fields
    private var mySerializationContext: SerializationCtx? = null
    override val serializationContext: SerializationCtx
        get() = mySerializationContext ?: throw IllegalStateException("Attempting to get serialization context too soon for $location")
    //initializer
    //secondary constructor
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter) {
        printer.println("InterningExtensionHolder (")
        printer.print(")")
    }
}


class InterningMtModel private constructor(
        val searchLabel: String,
        private val _signaller: RdSignal<String>
) : RdBindableBase() {
    //companion

    companion object : IMarshaller<InterningMtModel> {
        override val _type: KClass<InterningMtModel> = InterningMtModel::class

        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): InterningMtModel {
            val _id = RdId.read(buffer)
            val searchLabel = buffer.readString()
            val _signaller = RdSignal.read(ctx, buffer, __StringInternedAtTestSerializer)
            return InterningMtModel(searchLabel, _signaller).withId(_id).apply { mySerializationContext = ctx.withInternRootsHere(this, "Test") }
        }

        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: InterningMtModel) {
            value.rdid.write(buffer)
            buffer.writeString(value.searchLabel)
            RdSignal.write(ctx, buffer, value._signaller)
            value.mySerializationContext = ctx.withInternRootsHere(value, "Test")
        }

        private val __StringInternedAtTestSerializer = FrameworkMarshallers.String.interned("Test")
    }
    //fields
    val signaller: IAsyncSignal<String> get() = _signaller
    private var mySerializationContext: SerializationCtx? = null
    override val serializationContext: SerializationCtx
        get() = mySerializationContext ?: throw IllegalStateException("Attempting to get serialization context too soon for $location")
    //initializer
    init {
        _signaller.async = true
    }

    init {
        bindableChildren.add("signaller" to _signaller)
    }

    //secondary constructor
    constructor(
            searchLabel: String
    ) : this(
            searchLabel,
            RdSignal<String>(__StringInternedAtTestSerializer)
    )

    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter) {
        printer.println("InterningMtModel (")
        printer.indent {
            print("searchLabel = "); searchLabel.print(printer); println()
            print("signaller = "); _signaller.print(printer); println()
        }
        printer.print(")")
    }
}


data class InterningNestedTestModel (
        val value: String,
        val inner: InterningNestedTestModel?
) : IPrintable {
    //companion

    companion object : IMarshaller<InterningNestedTestModel> {
        override val _type: KClass<InterningNestedTestModel> = InterningNestedTestModel::class

        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): InterningNestedTestModel {
            val value = buffer.readString()
            val inner = buffer.readNullable { ctx.readInterned(buffer, "Test") { _, _ -> InterningNestedTestModel.read(ctx, buffer) } }
            return InterningNestedTestModel(value, inner)
        }

        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: InterningNestedTestModel) {
            buffer.writeString(value.value)
            buffer.writeNullable(value.inner) { ctx.writeInterned(buffer, it, "Test") { _, _, internedValue -> InterningNestedTestModel.write(ctx, buffer, internedValue) } }
        }

    }
    //fields
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other::class != this::class) return false

        other as InterningNestedTestModel

        if (value != other.value) return false
        if (inner != other.inner) return false

        return true
    }
    //hash code trait
    override fun hashCode(): Int {
        var __r = 0
        __r = __r*31 + value.hashCode()
        __r = __r*31 + if (inner != null) inner.hashCode() else 0
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter) {
        printer.println("InterningNestedTestModel (")
        printer.indent {
            print("value = "); value.print(printer); println()
            print("inner = "); inner.print(printer); println()
        }
        printer.print(")")
    }
}


data class InterningNestedTestStringModel (
        val value: String,
        val inner: InterningNestedTestStringModel?
) : IPrintable {
    //companion

    companion object : IMarshaller<InterningNestedTestStringModel> {
        override val _type: KClass<InterningNestedTestStringModel> = InterningNestedTestStringModel::class

        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): InterningNestedTestStringModel {
            val value = ctx.readInterned(buffer, "Test") { _, _ -> buffer.readString() }
            val inner = buffer.readNullable { InterningNestedTestStringModel.read(ctx, buffer) }
            return InterningNestedTestStringModel(value, inner)
        }

        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: InterningNestedTestStringModel) {
            ctx.writeInterned(buffer, value.value, "Test") { _, _, internedValue -> buffer.writeString(internedValue) }
            buffer.writeNullable(value.inner) { InterningNestedTestStringModel.write(ctx, buffer, it) }
        }

    }
    //fields
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other::class != this::class) return false

        other as InterningNestedTestStringModel

        if (value != other.value) return false
        if (inner != other.inner) return false

        return true
    }
    //hash code trait
    override fun hashCode(): Int {
        var __r = 0
        __r = __r*31 + value.hashCode()
        __r = __r*31 + if (inner != null) inner.hashCode() else 0
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter) {
        printer.println("InterningNestedTestStringModel (")
        printer.indent {
            print("value = "); value.print(printer); println()
            print("inner = "); inner.print(printer); println()
        }
        printer.print(")")
    }
}


class InterningProtocolLevelModel private constructor(
        val searchLabel: String,
        private val _issues: RdMap<Int, ProtocolWrappedStringModel>
) : RdBindableBase() {
    //companion

    companion object : IMarshaller<InterningProtocolLevelModel> {
        override val _type: KClass<InterningProtocolLevelModel> = InterningProtocolLevelModel::class

        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): InterningProtocolLevelModel {
            val _id = RdId.read(buffer)
            val searchLabel = buffer.readString()
            val _issues = RdMap.read(ctx, buffer, FrameworkMarshallers.Int, ProtocolWrappedStringModel)
            return InterningProtocolLevelModel(searchLabel, _issues).withId(_id)
        }

        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: InterningProtocolLevelModel) {
            value.rdid.write(buffer)
            buffer.writeString(value.searchLabel)
            RdMap.write(ctx, buffer, value._issues)
        }

    }
    //fields
    val issues: IMutableViewableMap<Int, ProtocolWrappedStringModel> get() = _issues
    //initializer
    init {
        _issues.optimizeNested = true
    }

    init {
        bindableChildren.add("issues" to _issues)
    }

    //secondary constructor
    constructor(
            searchLabel: String
    ) : this(
            searchLabel,
            RdMap<Int, ProtocolWrappedStringModel>(FrameworkMarshallers.Int, ProtocolWrappedStringModel)
    )

    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter) {
        printer.println("InterningProtocolLevelModel (")
        printer.indent {
            print("searchLabel = "); searchLabel.print(printer); println()
            print("issues = "); _issues.print(printer); println()
        }
        printer.print(")")
    }
}


class InterningTestModel private constructor(
        val searchLabel: String,
        private val _issues: RdMap<Int, WrappedStringModel>
) : RdBindableBase() {
    //companion

    companion object : IMarshaller<InterningTestModel> {
        override val _type: KClass<InterningTestModel> = InterningTestModel::class

        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): InterningTestModel {
            val _id = RdId.read(buffer)
            val searchLabel = buffer.readString()
            val _issues = RdMap.read(ctx, buffer, FrameworkMarshallers.Int, WrappedStringModel)
            return InterningTestModel(searchLabel, _issues).withId(_id).apply { mySerializationContext = ctx.withInternRootsHere(this, "Test") }
        }

        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: InterningTestModel) {
            value.rdid.write(buffer)
            buffer.writeString(value.searchLabel)
            RdMap.write(ctx, buffer, value._issues)
            value.mySerializationContext = ctx.withInternRootsHere(value, "Test")
        }

    }
    //fields
    val issues: IMutableViewableMap<Int, WrappedStringModel> get() = _issues
    private var mySerializationContext: SerializationCtx? = null
    override val serializationContext: SerializationCtx
        get() = mySerializationContext ?: throw IllegalStateException("Attempting to get serialization context too soon for $location")
    //initializer
    init {
        _issues.optimizeNested = true
    }

    init {
        bindableChildren.add("issues" to _issues)
    }

    //secondary constructor
    constructor(
            searchLabel: String
    ) : this(
            searchLabel,
            RdMap<Int, WrappedStringModel>(FrameworkMarshallers.Int, WrappedStringModel)
    )

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


data class ProtocolWrappedStringModel (
        val text: String
) : IPrintable {
    //companion

    companion object : IMarshaller<ProtocolWrappedStringModel> {
        override val _type: KClass<ProtocolWrappedStringModel> = ProtocolWrappedStringModel::class

        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): ProtocolWrappedStringModel {
            val text = ctx.readInterned(buffer, "Protocol") { _, _ -> buffer.readString() }
            return ProtocolWrappedStringModel(text)
        }

        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: ProtocolWrappedStringModel) {
            ctx.writeInterned(buffer, value.text, "Protocol") { _, _, internedValue -> buffer.writeString(internedValue) }
        }

    }
    //fields
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other::class != this::class) return false

        other as ProtocolWrappedStringModel

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
        printer.println("ProtocolWrappedStringModel (")
        printer.indent {
            print("text = "); text.print(printer); println()
        }
        printer.print(")")
    }
}


data class WrappedStringModel (
        val text: String
) : IPrintable {
    //companion

    companion object : IMarshaller<WrappedStringModel> {
        override val _type: KClass<WrappedStringModel> = WrappedStringModel::class

        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): WrappedStringModel {
            val text = ctx.readInterned(buffer, "Test") { _, _ -> buffer.readString() }
            return WrappedStringModel(text)
        }

        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: WrappedStringModel) {
            ctx.writeInterned(buffer, value.text, "Test") { _, _, internedValue -> buffer.writeString(internedValue) }
        }

    }
    //fields
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other::class != this::class) return false

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
