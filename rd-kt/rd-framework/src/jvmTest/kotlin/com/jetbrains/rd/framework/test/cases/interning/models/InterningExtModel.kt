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



class InterningExt private constructor(
        private val _root: RdOptionalProperty<InterningExtRootModel>
) : RdExtBase() {
    //companion

    companion object : ISerializersOwner {

        override fun registerSerializersCore(serializers: ISerializers) {
            serializers.register(InterningExtRootModel)
        }




        const val serializationHash = -2181600832385335602L
    }
    override val serializersOwner: ISerializersOwner get() = InterningExt
    override val serializationHash: Long get() = InterningExt.serializationHash

    //fields
    val root: IOptProperty<InterningExtRootModel> get() = _root
    //initializer
    init {
        bindableChildren.add("root" to _root)
    }

    //secondary constructor
    internal constructor(
    ) : this(
            RdOptionalProperty<InterningExtRootModel>(InterningExtRootModel)
    )

    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter) {
        printer.println("InterningExt (")
        printer.indent {
            print("root = "); _root.print(printer); println()
        }
        printer.print(")")
    }
}
val InterningExtensionHolder.interningExt get() = getOrCreateExtension("interningExt", ::InterningExt)



class InterningExtRootModel private constructor(
        private val _internedLocally: RdOptionalProperty<String>,
        private val _internedExternally: RdOptionalProperty<String>,
        private val _internedInProtocol: RdOptionalProperty<String>
) : RdBindableBase() {
    //companion

    companion object : IMarshaller<InterningExtRootModel> {
        override val _type: KClass<InterningExtRootModel> = InterningExtRootModel::class

        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): InterningExtRootModel {
            val _id = RdId.read(buffer)
            val _internedLocally = RdOptionalProperty.read(ctx, buffer, __StringInternedAtInternScopeInExtSerializer)
            val _internedExternally = RdOptionalProperty.read(ctx, buffer, __StringInternedAtInternScopeOutOfExtSerializer)
            val _internedInProtocol = RdOptionalProperty.read(ctx, buffer, __StringInternedAtProtocolSerializer)
            return InterningExtRootModel(_internedLocally, _internedExternally, _internedInProtocol).withId(_id).apply { mySerializationContext = ctx.withInternRootsHere(this, "InternScopeInExt") }
        }

        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: InterningExtRootModel) {
            value.rdid.write(buffer)
            RdOptionalProperty.write(ctx, buffer, value._internedLocally)
            RdOptionalProperty.write(ctx, buffer, value._internedExternally)
            RdOptionalProperty.write(ctx, buffer, value._internedInProtocol)
            value.mySerializationContext = ctx.withInternRootsHere(value, "InternScopeInExt")
        }

        private val __StringInternedAtInternScopeInExtSerializer = FrameworkMarshallers.String.interned("InternScopeInExt")
        private val __StringInternedAtInternScopeOutOfExtSerializer = FrameworkMarshallers.String.interned("InternScopeOutOfExt")
        private val __StringInternedAtProtocolSerializer = FrameworkMarshallers.String.interned("Protocol")
    }
    //fields
    val internedLocally: IOptProperty<String> get() = _internedLocally
    val internedExternally: IOptProperty<String> get() = _internedExternally
    val internedInProtocol: IOptProperty<String> get() = _internedInProtocol
    private var mySerializationContext: SerializationCtx? = null
    override val serializationContext: SerializationCtx
        get() = mySerializationContext ?: throw IllegalStateException("Attempting to get serialization context too soon for $location")
    //initializer
    init {
        _internedLocally.optimizeNested = true
        _internedExternally.optimizeNested = true
        _internedInProtocol.optimizeNested = true
    }

    init {
        bindableChildren.add("internedLocally" to _internedLocally)
        bindableChildren.add("internedExternally" to _internedExternally)
        bindableChildren.add("internedInProtocol" to _internedInProtocol)
    }

    //secondary constructor
    constructor(
    ) : this(
            RdOptionalProperty<String>(__StringInternedAtInternScopeInExtSerializer),
            RdOptionalProperty<String>(__StringInternedAtInternScopeOutOfExtSerializer),
            RdOptionalProperty<String>(__StringInternedAtProtocolSerializer)
    )

    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter) {
        printer.println("InterningExtRootModel (")
        printer.indent {
            print("internedLocally = "); _internedLocally.print(printer); println()
            print("internedExternally = "); _internedExternally.print(printer); println()
            print("internedInProtocol = "); _internedInProtocol.print(printer); println()
        }
        printer.print(")")
    }
}
