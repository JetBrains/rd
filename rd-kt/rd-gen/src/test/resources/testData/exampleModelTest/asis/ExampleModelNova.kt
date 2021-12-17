@file:Suppress("EXPERIMENTAL_API_USAGE","EXPERIMENTAL_UNSIGNED_LITERALS","PackageDirectoryMismatch","UnusedImport","unused","LocalVariableName","CanBeVal","PropertyName","EnumEntryName","ClassName","ObjectPropertyName","UnnecessaryVariable","SpellCheckingInspection")
package org.example

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.framework.impl.*

import com.jetbrains.rd.util.lifetime.*
import com.jetbrains.rd.util.reactive.*
import com.jetbrains.rd.util.string.*
import com.jetbrains.rd.util.*
import kotlin.reflect.KClass
import kotlin.jvm.JvmStatic



/**
 * #### Generated from [Example.kt:20]
 */
class ExampleModelNova private constructor(
    private val _push: RdSignal<Int>,
    private val _version: RdOptionalProperty<Int>,
    private val _documents: RdMap<Int, Document>,
    private val _editors: RdMap<ScalarExample, TextControl>
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            serializers.register(Selection)
            serializers.register(Baz)
            serializers.register(FooBar)
            serializers.register(Document)
            serializers.register(ScalarExample)
            serializers.register(TextControl)
            serializers.register(EnumSetTest.marshaller)
            serializers.register(Z.marshaller)
            serializers.register(Completion)
            serializers.register(Foo_Unknown)
            serializers.register(ScalarPrimer_Unknown)
            serializers.register(A_Unknown)
        }
        
        
        @JvmStatic
        @JvmName("internalCreateModel")
        @Deprecated("Use create instead", ReplaceWith("create(lifetime, protocol)"))
        internal fun createModel(lifetime: Lifetime, protocol: IProtocol): ExampleModelNova  {
            @Suppress("DEPRECATION")
            return create(lifetime, protocol)
        }
        
        @JvmStatic
        @Deprecated("Use protocol.exampleModelNova or revise the extension scope instead", ReplaceWith("protocol.exampleModelNova"))
        fun create(lifetime: Lifetime, protocol: IProtocol): ExampleModelNova  {
            ExampleRootNova.register(protocol.serializers)
            
            return ExampleModelNova().apply {
                identify(protocol.identity, RdId.Null.mix("ExampleModelNova"))
                bind(lifetime, protocol, "ExampleModelNova")
            }
        }
        
        
        const val serializationHash = -5738447821523408934L
        
    }
    override val serializersOwner: ISerializersOwner get() = ExampleModelNova
    override val serializationHash: Long get() = ExampleModelNova.serializationHash
    
    //fields
    val push: ISignal<Int> get() = _push
    val version: IOptPropertyView<Int> get() = _version
    val documents: IMutableViewableMap<Int, Document> get() = _documents
    val editors: IMutableViewableMap<ScalarExample, TextControl> get() = _editors
    //methods
    //initializer
    init {
        _version.optimizeNested = true
    }
    
    init {
        bindableChildren.add("push" to _push)
        bindableChildren.add("version" to _version)
        bindableChildren.add("documents" to _documents)
        bindableChildren.add("editors" to _editors)
    }
    
    //secondary constructor
    private constructor(
    ) : this(
        RdSignal<Int>(FrameworkMarshallers.Int),
        RdOptionalProperty<Int>(FrameworkMarshallers.Int),
        RdMap<Int, Document>(FrameworkMarshallers.Int, Document),
        RdMap<ScalarExample, TextControl>(ScalarExample, TextControl)
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("ExampleModelNova (")
        printer.indent {
            print("push = "); _push.print(printer); println()
            print("version = "); _version.print(printer); println()
            print("documents = "); _documents.print(printer); println()
            print("editors = "); _editors.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): ExampleModelNova   {
        return ExampleModelNova(
            _push.deepClonePolymorphic(),
            _version.deepClonePolymorphic(),
            _documents.deepClonePolymorphic(),
            _editors.deepClonePolymorphic()
        )
    }
    //contexts
}
val IProtocol.exampleModelNova get() = getOrCreateExtension(ExampleModelNova::class) { @Suppress("DEPRECATION") ExampleModelNova.create(lifetime, this) }



/**
 * #### Generated from [Example.kt:43]
 */
abstract class A (
    protected val _y: RdOptionalProperty<String>,
    protected val _z: RdOptionalProperty<Z>,
    x: Int,
    _sdf: RdMap<Int, Int>
) : Foo (
    x,
    _sdf
) {
    //companion
    
    companion object : IAbstractDeclaration<A> {
        override fun readUnknownInstance(ctx: SerializationCtx, buffer: AbstractBuffer, unknownId: RdId, size: Int): A  {
            val objectStartPosition = buffer.position
            val _id = RdId.read(buffer)
            val _y = RdOptionalProperty.read(ctx, buffer, FrameworkMarshallers.String)
            val _z = RdOptionalProperty.read(ctx, buffer, Z.marshaller)
            val x = buffer.readInt()
            val _sdf = RdMap.read(ctx, buffer, FrameworkMarshallers.Int, FrameworkMarshallers.Int)
            val unknownBytes = ByteArray(objectStartPosition + size - buffer.position)
            buffer.readByteArrayRaw(unknownBytes)
            return A_Unknown(_y, _z, x, _sdf, unknownId, unknownBytes).withId(_id)
        }
        
        
    }
    //fields
    val y: IOptProperty<String> get() = _y
    val z: IOptProperty<Z> get() = _z
    //methods
    //initializer
    init {
        _y.optimizeNested = true
        _z.optimizeNested = true
    }
    
    init {
        bindableChildren.add("y" to _y)
        bindableChildren.add("z" to _z)
    }
    
    //secondary constructor
    //equals trait
    //hash code trait
    //pretty print
    //deepClone
    //contexts
}


class A_Unknown (
    _y: RdOptionalProperty<String>,
    _z: RdOptionalProperty<Z>,
    x: Int,
    _sdf: RdMap<Int, Int>,
    override val unknownId: RdId,
    val unknownBytes: ByteArray
) : A (
    _y,
    _z,
    x,
    _sdf
), IUnknownInstance {
    //companion
    
    companion object : IMarshaller<A_Unknown> {
        override val _type: KClass<A_Unknown> = A_Unknown::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): A_Unknown  {
            throw NotImplementedError("Unknown instances should not be read via serializer")
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: A_Unknown)  {
            value.rdid.write(buffer)
            RdOptionalProperty.write(ctx, buffer, value._y)
            RdOptionalProperty.write(ctx, buffer, value._z)
            buffer.writeInt(value.x)
            RdMap.write(ctx, buffer, value._sdf)
            buffer.writeByteArrayRaw(value.unknownBytes)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    constructor(
        x: Int,
        unknownId: RdId,
        unknownBytes: ByteArray
    ) : this(
        RdOptionalProperty<String>(FrameworkMarshallers.String),
        RdOptionalProperty<Z>(Z.marshaller),
        x,
        RdMap<Int, Int>(FrameworkMarshallers.Int, FrameworkMarshallers.Int),
        unknownId,
        unknownBytes
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("A_Unknown (")
        printer.indent {
            print("y = "); _y.print(printer); println()
            print("z = "); _z.print(printer); println()
            print("x = "); x.print(printer); println()
            print("sdf = "); _sdf.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): A_Unknown   {
        return A_Unknown(
            _y.deepClonePolymorphic(),
            _z.deepClonePolymorphic(),
            x,
            _sdf.deepClonePolymorphic(),
            unknownId,
            unknownBytes
        )
    }
    //contexts
}


/**
 * #### Generated from [Example.kt:51]
 */
class Baz private constructor(
    val foo: List<Foo>,
    val bar: List<A?>,
    val nls_field: @org.jetbrains.annotations.Nls String,
    val nls_nullable_field: @org.jetbrains.annotations.Nls String?,
    val string_list_field: List<String>,
    val nls_list_field: List<@org.jetbrains.annotations.Nls String>,
    private val _foo1: RdProperty<Foo?>,
    private val _bar1: RdProperty<A?>,
    private val _mapScalar: RdMap<Int, ScalarPrimer>,
    private val _mapBindable: RdMap<Int, FooBar>,
    private val _property_with_default_nls: RdProperty<@org.jetbrains.annotations.Nls String>,
    private val _property_with_several_attrs: RdOptionalProperty<@org.jetbrains.annotations.Nls @org.jetbrains.annotations.NonNls String>,
    private val _nls_prop: RdOptionalProperty<@org.jetbrains.annotations.Nls String>,
    private val _nullable_nls_prop: RdProperty<@org.jetbrains.annotations.Nls String?>,
    val non_nls_open_field: @org.jetbrains.annotations.NonNls String,
    _y: RdOptionalProperty<String>,
    _z: RdOptionalProperty<Z>,
    x: Int,
    _sdf: RdMap<Int, Int>
) : A (
    _y,
    _z,
    x,
    _sdf
) {
    //companion
    
    companion object : IMarshaller<Baz> {
        override val _type: KClass<Baz> = Baz::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): Baz  {
            val _id = RdId.read(buffer)
            val _y = RdOptionalProperty.read(ctx, buffer, FrameworkMarshallers.String)
            val _z = RdOptionalProperty.read(ctx, buffer, Z.marshaller)
            val x = buffer.readInt()
            val _sdf = RdMap.read(ctx, buffer, FrameworkMarshallers.Int, FrameworkMarshallers.Int)
            val foo = buffer.readList { ctx.serializers.readPolymorphic<Foo>(ctx, buffer, Foo) }
            val bar = buffer.readList { buffer.readNullable { ctx.serializers.readPolymorphic<A>(ctx, buffer, A) } }
            val nls_field = buffer.readString()
            val nls_nullable_field = buffer.readNullable { buffer.readString() }
            val string_list_field = buffer.readList { buffer.readString() }
            val nls_list_field = buffer.readList { buffer.readString() }
            val _foo1 = RdProperty.read(ctx, buffer, __FooNullableSerializer)
            val _bar1 = RdProperty.read(ctx, buffer, __ANullableSerializer)
            val _mapScalar = RdMap.read(ctx, buffer, FrameworkMarshallers.Int, AbstractPolymorphic(ScalarPrimer))
            val _mapBindable = RdMap.read(ctx, buffer, FrameworkMarshallers.Int, FooBar)
            val _property_with_default_nls = RdProperty.read(ctx, buffer, __StringSerializer)
            val _property_with_several_attrs = RdOptionalProperty.read(ctx, buffer, __StringSerializer)
            val _nls_prop = RdOptionalProperty.read(ctx, buffer, __StringSerializer)
            val _nullable_nls_prop = RdProperty.read(ctx, buffer, __StringNullableSerializer)
            val non_nls_open_field = buffer.readString()
            return Baz(foo, bar, nls_field, nls_nullable_field, string_list_field, nls_list_field, _foo1, _bar1, _mapScalar, _mapBindable, _property_with_default_nls, _property_with_several_attrs, _nls_prop, _nullable_nls_prop, non_nls_open_field, _y, _z, x, _sdf).withId(_id)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: Baz)  {
            value.rdid.write(buffer)
            RdOptionalProperty.write(ctx, buffer, value._y)
            RdOptionalProperty.write(ctx, buffer, value._z)
            buffer.writeInt(value.x)
            RdMap.write(ctx, buffer, value._sdf)
            buffer.writeList(value.foo) { v -> ctx.serializers.writePolymorphic(ctx, buffer, v) }
            buffer.writeList(value.bar) { v -> buffer.writeNullable(v) { ctx.serializers.writePolymorphic(ctx, buffer, it) } }
            buffer.writeString(value.nls_field)
            buffer.writeNullable(value.nls_nullable_field) { buffer.writeString(it) }
            buffer.writeList(value.string_list_field) { v -> buffer.writeString(v) }
            buffer.writeList(value.nls_list_field) { v -> buffer.writeString(v) }
            RdProperty.write(ctx, buffer, value._foo1)
            RdProperty.write(ctx, buffer, value._bar1)
            RdMap.write(ctx, buffer, value._mapScalar)
            RdMap.write(ctx, buffer, value._mapBindable)
            RdProperty.write(ctx, buffer, value._property_with_default_nls)
            RdOptionalProperty.write(ctx, buffer, value._property_with_several_attrs)
            RdOptionalProperty.write(ctx, buffer, value._nls_prop)
            RdProperty.write(ctx, buffer, value._nullable_nls_prop)
            buffer.writeString(value.non_nls_open_field)
        }
        
        private val __FooNullableSerializer = AbstractPolymorphic(Foo).nullable()
        private val __ANullableSerializer = AbstractPolymorphic(A).nullable()
        private val __StringSerializer = FrameworkMarshallers.String
        private val __StringNullableSerializer = FrameworkMarshallers.String.nullable()
        
        const val const_nls : @org.jetbrains.annotations.Nls String = "const_nls_value"
        const val const_for_default_nls : @org.jetbrains.annotations.Nls String = "291"
    }
    //fields
    val foo1: IProperty<Foo?> get() = _foo1
    val bar1: IProperty<A?> get() = _bar1
    val mapScalar: IAsyncViewableMap<Int, ScalarPrimer> get() = _mapScalar
    val mapBindable: IMutableViewableMap<Int, FooBar> get() = _mapBindable
    val property_with_default_nls: IProperty<@org.jetbrains.annotations.Nls String> get() = _property_with_default_nls
    val property_with_several_attrs: IOptProperty<@org.jetbrains.annotations.Nls @org.jetbrains.annotations.NonNls String> get() = _property_with_several_attrs
    val nls_prop: IOptProperty<@org.jetbrains.annotations.Nls String> get() = _nls_prop
    val nullable_nls_prop: IProperty<@org.jetbrains.annotations.Nls String?> get() = _nullable_nls_prop
    //methods
    //initializer
    init {
        _mapScalar.optimizeNested = true
        _property_with_default_nls.optimizeNested = true
        _property_with_several_attrs.optimizeNested = true
        _nls_prop.optimizeNested = true
        _nullable_nls_prop.optimizeNested = true
    }
    
    init {
        _mapScalar.async = true
    }
    
    init {
        bindableChildren.add("foo" to foo)
        bindableChildren.add("bar" to bar)
        bindableChildren.add("foo1" to _foo1)
        bindableChildren.add("bar1" to _bar1)
        bindableChildren.add("mapScalar" to _mapScalar)
        bindableChildren.add("mapBindable" to _mapBindable)
        bindableChildren.add("property_with_default_nls" to _property_with_default_nls)
        bindableChildren.add("property_with_several_attrs" to _property_with_several_attrs)
        bindableChildren.add("nls_prop" to _nls_prop)
        bindableChildren.add("nullable_nls_prop" to _nullable_nls_prop)
    }
    
    //secondary constructor
    constructor(
        foo: List<Foo>,
        bar: List<A?>,
        nls_field: @org.jetbrains.annotations.Nls String,
        nls_nullable_field: @org.jetbrains.annotations.Nls String?,
        string_list_field: List<String>,
        nls_list_field: List<@org.jetbrains.annotations.Nls String>,
        non_nls_open_field: @org.jetbrains.annotations.NonNls String,
        x: Int
    ) : this(
        foo,
        bar,
        nls_field,
        nls_nullable_field,
        string_list_field,
        nls_list_field,
        RdProperty<Foo?>(null, __FooNullableSerializer),
        RdProperty<A?>(null, __ANullableSerializer),
        RdMap<Int, ScalarPrimer>(FrameworkMarshallers.Int, AbstractPolymorphic(ScalarPrimer)),
        RdMap<Int, FooBar>(FrameworkMarshallers.Int, FooBar),
        RdProperty<@org.jetbrains.annotations.Nls String>(const_for_default_nls, __StringSerializer),
        RdOptionalProperty<@org.jetbrains.annotations.Nls @org.jetbrains.annotations.NonNls String>(__StringSerializer),
        RdOptionalProperty<@org.jetbrains.annotations.Nls String>(__StringSerializer),
        RdProperty<@org.jetbrains.annotations.Nls String?>(null, __StringNullableSerializer),
        non_nls_open_field,
        RdOptionalProperty<String>(FrameworkMarshallers.String),
        RdOptionalProperty<Z>(Z.marshaller),
        x,
        RdMap<Int, Int>(FrameworkMarshallers.Int, FrameworkMarshallers.Int)
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("Baz (")
        printer.indent {
            print("foo = "); foo.print(printer); println()
            print("bar = "); bar.print(printer); println()
            print("nls_field = "); nls_field.print(printer); println()
            print("nls_nullable_field = "); nls_nullable_field.print(printer); println()
            print("string_list_field = "); string_list_field.print(printer); println()
            print("nls_list_field = "); nls_list_field.print(printer); println()
            print("foo1 = "); _foo1.print(printer); println()
            print("bar1 = "); _bar1.print(printer); println()
            print("mapScalar = "); _mapScalar.print(printer); println()
            print("mapBindable = "); _mapBindable.print(printer); println()
            print("property_with_default_nls = "); _property_with_default_nls.print(printer); println()
            print("property_with_several_attrs = "); _property_with_several_attrs.print(printer); println()
            print("nls_prop = "); _nls_prop.print(printer); println()
            print("nullable_nls_prop = "); _nullable_nls_prop.print(printer); println()
            print("non_nls_open_field = "); non_nls_open_field.print(printer); println()
            print("y = "); _y.print(printer); println()
            print("z = "); _z.print(printer); println()
            print("x = "); x.print(printer); println()
            print("sdf = "); _sdf.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): Baz   {
        return Baz(
            foo.deepClonePolymorphic(),
            bar.deepClonePolymorphic(),
            nls_field,
            nls_nullable_field,
            string_list_field,
            nls_list_field,
            _foo1.deepClonePolymorphic(),
            _bar1.deepClonePolymorphic(),
            _mapScalar.deepClonePolymorphic(),
            _mapBindable.deepClonePolymorphic(),
            _property_with_default_nls.deepClonePolymorphic(),
            _property_with_several_attrs.deepClonePolymorphic(),
            _nls_prop.deepClonePolymorphic(),
            _nullable_nls_prop.deepClonePolymorphic(),
            non_nls_open_field,
            _y.deepClonePolymorphic(),
            _z.deepClonePolymorphic(),
            x,
            _sdf.deepClonePolymorphic()
        )
    }
    //contexts
}


/**
 * #### Generated from [Example.kt:89]
 */
class Completion private constructor(
    private val _lookupItems: RdMap<Int, Boolean>
) : RdBindableBase() {
    //companion
    
    companion object : IMarshaller<Completion> {
        override val _type: KClass<Completion> = Completion::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): Completion  {
            val _id = RdId.read(buffer)
            val _lookupItems = RdMap.read(ctx, buffer, FrameworkMarshallers.Int, FrameworkMarshallers.Bool)
            return Completion(_lookupItems).withId(_id)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: Completion)  {
            value.rdid.write(buffer)
            RdMap.write(ctx, buffer, value._lookupItems)
        }
        
        
    }
    //fields
    val lookupItems: IMutableViewableMap<Int, Boolean> get() = _lookupItems
    //methods
    //initializer
    init {
        _lookupItems.optimizeNested = true
    }
    
    init {
        bindableChildren.add("lookupItems" to _lookupItems)
    }
    
    //secondary constructor
    constructor(
    ) : this(
        RdMap<Int, Boolean>(FrameworkMarshallers.Int, FrameworkMarshallers.Bool)
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("Completion (")
        printer.indent {
            print("lookupItems = "); _lookupItems.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): Completion   {
        return Completion(
            _lookupItems.deepClonePolymorphic()
        )
    }
    //contexts
}


/**
 * #### Generated from [Example.kt:84]
 */
class Document private constructor(
    val moniker: FooBar,
    val buffer: String?,
    private val _andBackAgain: RdCall<String, Int>,
    val completion: Completion,
    val arr1: ByteArray,
    val arr2: Array<BooleanArray>
) : RdBindableBase() {
    //companion
    
    companion object : IMarshaller<Document> {
        override val _type: KClass<Document> = Document::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): Document  {
            val _id = RdId.read(buffer)
            val moniker = FooBar.read(ctx, buffer)
            val buffer_ = buffer.readNullable { buffer.readString() }
            val _andBackAgain = RdCall.read(ctx, buffer, FrameworkMarshallers.String, FrameworkMarshallers.Int)
            val completion = Completion.read(ctx, buffer)
            val arr1 = buffer.readByteArray()
            val arr2 = buffer.readArray {buffer.readBooleanArray()}
            return Document(moniker, buffer_, _andBackAgain, completion, arr1, arr2).withId(_id)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: Document)  {
            value.rdid.write(buffer)
            FooBar.write(ctx, buffer, value.moniker)
            buffer.writeNullable(value.buffer) { buffer.writeString(it) }
            RdCall.write(ctx, buffer, value._andBackAgain)
            Completion.write(ctx, buffer, value.completion)
            buffer.writeByteArray(value.arr1)
            buffer.writeArray(value.arr2) { buffer.writeBooleanArray(it) }
        }
        
        
    }
    //fields
    val andBackAgain: IRdEndpoint<String, Int> get() = _andBackAgain
    //methods
    //initializer
    init {
        bindableChildren.add("moniker" to moniker)
        bindableChildren.add("andBackAgain" to _andBackAgain)
        bindableChildren.add("completion" to completion)
    }
    
    //secondary constructor
    constructor(
        moniker: FooBar,
        buffer: String?,
        arr1: ByteArray,
        arr2: Array<BooleanArray>
    ) : this(
        moniker,
        buffer,
        RdCall<String, Int>(FrameworkMarshallers.String, FrameworkMarshallers.Int),
        Completion(),
        arr1,
        arr2
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("Document (")
        printer.indent {
            print("moniker = "); moniker.print(printer); println()
            print("buffer = "); buffer.print(printer); println()
            print("andBackAgain = "); _andBackAgain.print(printer); println()
            print("completion = "); completion.print(printer); println()
            print("arr1 = "); arr1.print(printer); println()
            print("arr2 = "); arr2.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): Document   {
        return Document(
            moniker.deepClonePolymorphic(),
            buffer,
            _andBackAgain.deepClonePolymorphic(),
            completion.deepClonePolymorphic(),
            arr1,
            arr2
        )
    }
    //contexts
}


/**
 * #### Generated from [Example.kt:26]
 */
enum class EnumSetTest {
    a, 
    b, 
    c;
    
    companion object {
        val marshaller = FrameworkMarshallers.enumSet<EnumSetTest>()
        
    }
}


/**
 * #### Generated from [Example.kt:34]
 */
abstract class Foo (
    val x: Int,
    protected val _sdf: RdMap<Int, Int>
) : RdBindableBase() {
    //companion
    
    companion object : IAbstractDeclaration<Foo> {
        override fun readUnknownInstance(ctx: SerializationCtx, buffer: AbstractBuffer, unknownId: RdId, size: Int): Foo  {
            val objectStartPosition = buffer.position
            val _id = RdId.read(buffer)
            val x = buffer.readInt()
            val _sdf = RdMap.read(ctx, buffer, FrameworkMarshallers.Int, FrameworkMarshallers.Int)
            val unknownBytes = ByteArray(objectStartPosition + size - buffer.position)
            buffer.readByteArrayRaw(unknownBytes)
            return Foo_Unknown(x, _sdf, unknownId, unknownBytes).withId(_id)
        }
        
        
    }
    //fields
    val sdf: IMutableViewableMap<Int, Int> get() = _sdf
    //methods
    //initializer
    init {
        _sdf.optimizeNested = true
    }
    
    init {
        bindableChildren.add("sdf" to _sdf)
    }
    
    //secondary constructor
    //equals trait
    //hash code trait
    //pretty print
    //deepClone
    //contexts
}


/**
 * #### Generated from [Example.kt:73]
 */
class FooBar (
    val a: Baz
) : RdBindableBase() {
    //companion
    
    companion object : IMarshaller<FooBar> {
        override val _type: KClass<FooBar> = FooBar::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): FooBar  {
            val _id = RdId.read(buffer)
            val a = Baz.read(ctx, buffer)
            return FooBar(a).withId(_id)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: FooBar)  {
            value.rdid.write(buffer)
            Baz.write(ctx, buffer, value.a)
        }
        
        
    }
    //fields
    //methods
    //initializer
    init {
        bindableChildren.add("a" to a)
    }
    
    //secondary constructor
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("FooBar (")
        printer.indent {
            print("a = "); a.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): FooBar   {
        return FooBar(
            a.deepClonePolymorphic()
        )
    }
    //contexts
}


class Foo_Unknown (
    x: Int,
    _sdf: RdMap<Int, Int>,
    override val unknownId: RdId,
    val unknownBytes: ByteArray
) : Foo (
    x,
    _sdf
), IUnknownInstance {
    //companion
    
    companion object : IMarshaller<Foo_Unknown> {
        override val _type: KClass<Foo_Unknown> = Foo_Unknown::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): Foo_Unknown  {
            throw NotImplementedError("Unknown instances should not be read via serializer")
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: Foo_Unknown)  {
            value.rdid.write(buffer)
            buffer.writeInt(value.x)
            RdMap.write(ctx, buffer, value._sdf)
            buffer.writeByteArrayRaw(value.unknownBytes)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    constructor(
        x: Int,
        unknownId: RdId,
        unknownBytes: ByteArray
    ) : this(
        x,
        RdMap<Int, Int>(FrameworkMarshallers.Int, FrameworkMarshallers.Int),
        unknownId,
        unknownBytes
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("Foo_Unknown (")
        printer.indent {
            print("x = "); x.print(printer); println()
            print("sdf = "); _sdf.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): Foo_Unknown   {
        return Foo_Unknown(
            x,
            _sdf.deepClonePolymorphic(),
            unknownId,
            unknownBytes
        )
    }
    //contexts
}


/**
 * #### Generated from [Example.kt:98]
 */
data class ScalarExample (
    val intfield: Int
) : IPrintable {
    //companion
    
    companion object : IMarshaller<ScalarExample> {
        override val _type: KClass<ScalarExample> = ScalarExample::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): ScalarExample  {
            val intfield = buffer.readInt()
            return ScalarExample(intfield)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: ScalarExample)  {
            buffer.writeInt(value.intfield)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as ScalarExample
        
        if (intfield != other.intfield) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + intfield.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("ScalarExample (")
        printer.indent {
            print("intfield = "); intfield.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [Example.kt:39]
 */
abstract class ScalarPrimer (
    val x: Int
) : IPrintable {
    //companion
    
    companion object : IAbstractDeclaration<ScalarPrimer> {
        override fun readUnknownInstance(ctx: SerializationCtx, buffer: AbstractBuffer, unknownId: RdId, size: Int): ScalarPrimer  {
            val objectStartPosition = buffer.position
            val x = buffer.readInt()
            val unknownBytes = ByteArray(objectStartPosition + size - buffer.position)
            buffer.readByteArrayRaw(unknownBytes)
            return ScalarPrimer_Unknown(x, unknownId, unknownBytes)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    //hash code trait
    //pretty print
    //deepClone
    //contexts
}


class ScalarPrimer_Unknown (
    x: Int,
    override val unknownId: RdId,
    val unknownBytes: ByteArray
) : ScalarPrimer (
    x
), IUnknownInstance {
    //companion
    
    companion object : IMarshaller<ScalarPrimer_Unknown> {
        override val _type: KClass<ScalarPrimer_Unknown> = ScalarPrimer_Unknown::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): ScalarPrimer_Unknown  {
            throw NotImplementedError("Unknown instances should not be read via serializer")
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: ScalarPrimer_Unknown)  {
            buffer.writeInt(value.x)
            buffer.writeByteArrayRaw(value.unknownBytes)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as ScalarPrimer_Unknown
        
        if (x != other.x) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + x.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("ScalarPrimer_Unknown (")
        printer.indent {
            print("x = "); x.print(printer); println()
        }
        printer.print(")")
    }
    
    override fun toString() = PrettyPrinter().singleLine().also { print(it) }.toString()
    //deepClone
    //contexts
}


/**
 * #### Generated from [Example.kt:22]
 */
data class Selection (
    val start: Int,
    val end: Int,
    val lst: IntArray,
    val enumSetTest: EnumSet<EnumSetTest>,
    val nls_field: @org.jetbrains.annotations.Nls String
) : IPrintable {
    //companion
    
    companion object : IMarshaller<Selection> {
        override val _type: KClass<Selection> = Selection::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): Selection  {
            val start = buffer.readInt()
            val end = buffer.readInt()
            val lst = buffer.readIntArray()
            val enumSetTest = buffer.readEnumSet<EnumSetTest>()
            val nls_field = buffer.readString()
            return Selection(start, end, lst, enumSetTest, nls_field)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: Selection)  {
            buffer.writeInt(value.start)
            buffer.writeInt(value.end)
            buffer.writeIntArray(value.lst)
            buffer.writeEnumSet(value.enumSetTest)
            buffer.writeString(value.nls_field)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as Selection
        
        if (start != other.start) return false
        if (end != other.end) return false
        if (!(lst contentEquals other.lst)) return false
        if (enumSetTest != other.enumSetTest) return false
        if (nls_field != other.nls_field) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + start.hashCode()
        __r = __r*31 + end.hashCode()
        __r = __r*31 + lst.contentHashCode()
        __r = __r*31 + enumSetTest.hashCode()
        __r = __r*31 + nls_field.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("Selection (")
        printer.indent {
            print("start = "); start.print(printer); println()
            print("end = "); end.print(printer); println()
            print("lst = "); lst.print(printer); println()
            print("enumSetTest = "); enumSetTest.print(printer); println()
            print("nls_field = "); nls_field.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [Example.kt:101]
 */
class TextControl private constructor(
    private val _selection: RdOptionalProperty<Selection>,
    private val _vsink: RdSignal<Unit>,
    private val _vsource: RdSignal<Unit>,
    private val _there1: RdCall<Int, String>
) : RdBindableBase() {
    //companion
    
    companion object : IMarshaller<TextControl> {
        override val _type: KClass<TextControl> = TextControl::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): TextControl  {
            val _id = RdId.read(buffer)
            val _selection = RdOptionalProperty.read(ctx, buffer, Selection)
            val _vsink = RdSignal.read(ctx, buffer, FrameworkMarshallers.Void)
            val _vsource = RdSignal.read(ctx, buffer, FrameworkMarshallers.Void)
            val _there1 = RdCall.read(ctx, buffer, FrameworkMarshallers.Int, FrameworkMarshallers.String)
            return TextControl(_selection, _vsink, _vsource, _there1).withId(_id)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: TextControl)  {
            value.rdid.write(buffer)
            RdOptionalProperty.write(ctx, buffer, value._selection)
            RdSignal.write(ctx, buffer, value._vsink)
            RdSignal.write(ctx, buffer, value._vsource)
            RdCall.write(ctx, buffer, value._there1)
        }
        
        
    }
    //fields
    val selection: IOptPropertyView<Selection> get() = _selection
    val vsink: ISource<Unit> get() = _vsink
    val vsource: ISignal<Unit> get() = _vsource
    val there1: IRdCall<Int, String> get() = _there1
    //methods
    //initializer
    init {
        _selection.optimizeNested = true
    }
    
    init {
        bindableChildren.add("selection" to _selection)
        bindableChildren.add("vsink" to _vsink)
        bindableChildren.add("vsource" to _vsource)
        bindableChildren.add("there1" to _there1)
    }
    
    //secondary constructor
    constructor(
    ) : this(
        RdOptionalProperty<Selection>(Selection),
        RdSignal<Unit>(FrameworkMarshallers.Void),
        RdSignal<Unit>(FrameworkMarshallers.Void),
        RdCall<Int, String>(FrameworkMarshallers.Int, FrameworkMarshallers.String)
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("TextControl (")
        printer.indent {
            print("selection = "); _selection.print(printer); println()
            print("vsink = "); _vsink.print(printer); println()
            print("vsource = "); _vsource.print(printer); println()
            print("there1 = "); _there1.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): TextControl   {
        return TextControl(
            _selection.deepClonePolymorphic(),
            _vsink.deepClonePolymorphic(),
            _vsource.deepClonePolymorphic(),
            _there1.deepClonePolymorphic()
        )
    }
    //contexts
}


/**
 * #### Generated from [Example.kt:45]
 */
enum class Z {
    Bar, 
    z1;
    
    companion object {
        val marshaller = FrameworkMarshallers.enum<Z>()
        
    }
}
