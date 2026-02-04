@file:Suppress("EXPERIMENTAL_API_USAGE","EXPERIMENTAL_UNSIGNED_LITERALS","PackageDirectoryMismatch","UnusedImport","unused","LocalVariableName","CanBeVal","PropertyName","EnumEntryName","ClassName","ObjectPropertyName","UnnecessaryVariable","SpellCheckingInspection")
package org.example

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.framework.impl.*

import com.jetbrains.rd.util.lifetime.*
import com.jetbrains.rd.util.reactive.*
import com.jetbrains.rd.util.string.*
import com.jetbrains.rd.util.*
import kotlin.time.Duration
import kotlin.reflect.KClass
import kotlin.jvm.JvmStatic



/**
 * #### Generated from [Example.kt:21]
 */
class ExampleModelNova private constructor(
    private val _push: RdSignal<Int>,
    private val _version: RdOptionalProperty<Int>,
    private val _documents: RdMap<Int, Document>,
    private val _editors: RdMap<ScalarExample, TextControl>,
    private val _nonNullableStruct: RdOptionalProperty<UseStructTest>,
    private val _nullableStruct: RdProperty<UseStructTest?>
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            val classLoader = javaClass.classLoader
            serializers.register(LazyCompanionMarshaller(RdId(576020388116153), classLoader, "org.example.Selection"))
            serializers.register(LazyCompanionMarshaller(RdId(632584), classLoader, "org.example.Baz"))
            serializers.register(LazyCompanionMarshaller(RdId(18972494688), classLoader, "org.example.FooBar"))
            serializers.register(LazyCompanionMarshaller(RdId(609144101), classLoader, "org.example.Class"))
            serializers.register(LazyCompanionMarshaller(RdId(19349429704), classLoader, "org.example.Struct"))
            serializers.register(LazyCompanionMarshaller(RdId(572905478059643), classLoader, "org.example.OpenClass"))
            serializers.register(LazyCompanionMarshaller(RdId(17760070285811506), classLoader, "org.example.OpenStruct"))
            serializers.register(LazyCompanionMarshaller(RdId(-1667485286246826738), classLoader, "org.example.DerivedClass"))
            serializers.register(LazyCompanionMarshaller(RdId(3648188347942988543), classLoader, "org.example.DerivedStruct"))
            serializers.register(LazyCompanionMarshaller(RdId(-5037012260488689180), classLoader, "org.example.DerivedOpenClass"))
            serializers.register(LazyCompanionMarshaller(RdId(-8573427485006989079), classLoader, "org.example.DerivedOpenStruct"))
            serializers.register(LazyCompanionMarshaller(RdId(4287876202302424743), classLoader, "org.example.DerivedStructWith2Interfaces"))
            serializers.register(LazyCompanionMarshaller(RdId(555909160394251923), classLoader, "org.example.ValueStruct"))
            serializers.register(LazyCompanionMarshaller(RdId(-1063807896803205733), classLoader, "org.example.UseStructTest"))
            serializers.register(LazyCompanionMarshaller(RdId(18177246065230), classLoader, "org.example.Document"))
            serializers.register(LazyCompanionMarshaller(RdId(-3048302864262156661), classLoader, "org.example.ScalarExample"))
            serializers.register(LazyCompanionMarshaller(RdId(554385840109775197), classLoader, "org.example.TextControl"))
            serializers.register(LazyCompanionMarshaller(RdId(542326635061440960), classLoader, "org.example.EnumSetTest"))
            serializers.register(LazyCompanionMarshaller(RdId(679), classLoader, "org.example.Z"))
            serializers.register(LazyCompanionMarshaller(RdId(17442164506690639), classLoader, "org.example.Completion"))
            serializers.register(LazyCompanionMarshaller(RdId(543167202472902558), classLoader, "org.example.Foo_Unknown"))
            serializers.register(LazyCompanionMarshaller(RdId(-427415512200834691), classLoader, "org.example.ScalarPrimer_Unknown"))
            serializers.register(LazyCompanionMarshaller(RdId(560483126050681), classLoader, "org.example.A_Unknown"))
            serializers.register(LazyCompanionMarshaller(RdId(-8929432671501473473), classLoader, "org.example.BaseClass_Unknown"))
            serializers.register(LazyCompanionMarshaller(RdId(-8524399809491771292), classLoader, "org.example.BaseStruct_Unknown"))
            serializers.register(LazyCompanionMarshaller(RdId(-5669233277524003226), classLoader, "org.example.OpenClass_Unknown"))
            serializers.register(LazyCompanionMarshaller(RdId(308061035262048285), classLoader, "org.example.OpenStruct_Unknown"))
            serializers.register(LazyCompanionMarshaller(RdId(-5977551086017277745), classLoader, "org.example.DerivedOpenClass_Unknown"))
            serializers.register(LazyCompanionMarshaller(RdId(9196953045680089812), classLoader, "org.example.DerivedOpenStruct_Unknown"))
            serializers.register(LazyCompanionMarshaller(RdId(9208993593714803624), classLoader, "org.example.DerivedBaseClass_Unknown"))
            serializers.register(LazyCompanionMarshaller(RdId(364492200926270235), classLoader, "org.example.DerivedBaseStruct_Unknown"))
            serializers.register(LazyCompanionMarshaller(RdId(1299733233306533130), classLoader, "org.example.BaseClassWithInterface_Unknown"))
            serializers.register(LazyCompanionMarshaller(RdId(-2819848318270682619), classLoader, "org.example.BaseStructWithInterface_Unknown"))
            serializers.register(LazyCompanionMarshaller(RdId(-485232411962447965), classLoader, "org.example.DerivedClassWith2Interfaces_Unknown"))
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
            
            return ExampleModelNova()
        }
        
        private val __UseStructTestNullableSerializer = UseStructTest.nullable()
        
        const val serializationHash = -4242378315094463055L
        
    }
    override val serializersOwner: ISerializersOwner get() = ExampleModelNova
    override val serializationHash: Long get() = ExampleModelNova.serializationHash
    
    //fields
    val push: ISource<Int> get() = _push
    val version: IOptProperty<Int> get() = _version
    val documents: IMutableViewableMap<Int, Document> get() = _documents
    val editors: IMutableViewableMap<ScalarExample, TextControl> get() = _editors
    val nonNullableStruct: IOptProperty<UseStructTest> get() = _nonNullableStruct
    val nullableStruct: IProperty<UseStructTest?> get() = _nullableStruct
    //methods
    //initializer
    init {
        _version.optimizeNested = true
        _nonNullableStruct.optimizeNested = true
        _nullableStruct.optimizeNested = true
    }
    
    init {
        bindableChildren.add("push" to _push)
        bindableChildren.add("version" to _version)
        bindableChildren.add("documents" to _documents)
        bindableChildren.add("editors" to _editors)
        bindableChildren.add("nonNullableStruct" to _nonNullableStruct)
        bindableChildren.add("nullableStruct" to _nullableStruct)
    }
    
    //secondary constructor
    private constructor(
    ) : this(
        RdSignal<Int>(FrameworkMarshallers.Int),
        RdOptionalProperty<Int>(FrameworkMarshallers.Int),
        RdMap<Int, Document>(FrameworkMarshallers.Int, Document),
        RdMap<ScalarExample, TextControl>(ScalarExample, TextControl),
        RdOptionalProperty<UseStructTest>(UseStructTest),
        RdProperty<UseStructTest?>(null, __UseStructTestNullableSerializer)
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
            print("nonNullableStruct = "); _nonNullableStruct.print(printer); println()
            print("nullableStruct = "); _nullableStruct.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): ExampleModelNova   {
        return ExampleModelNova(
            _push.deepClonePolymorphic(),
            _version.deepClonePolymorphic(),
            _documents.deepClonePolymorphic(),
            _editors.deepClonePolymorphic(),
            _nonNullableStruct.deepClonePolymorphic(),
            _nullableStruct.deepClonePolymorphic()
        )
    }
    //contexts
    //threading
    override val extThreading: ExtThreadingKind get() = ExtThreadingKind.Default
}
val IProtocol.exampleModelNova get() = getOrCreateExtension(ExampleModelNova::class) { @Suppress("DEPRECATION") ExampleModelNova.create(lifetime, this) }



/**
 * #### Generated from [Example.kt:44]
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
    //threading
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
    //write-marshaller
    private fun write(ctx: SerializationCtx, buffer: AbstractBuffer)  {
        rdid.write(buffer)
        RdOptionalProperty.write(ctx, buffer, _y)
        RdOptionalProperty.write(ctx, buffer, _z)
        buffer.writeInt(x)
        RdMap.write(ctx, buffer, _sdf)
        buffer.writeByteArrayRaw(unknownBytes)
    }
    //companion
    
    companion object : IMarshaller<A_Unknown> {
        override val _type: KClass<A_Unknown> = A_Unknown::class
        override val id: RdId get() = RdId(560483126050681)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): A_Unknown  {
            throw NotImplementedError("Unknown instances should not be read via serializer")
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: A_Unknown)  {
            value.write(ctx, buffer)
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
    //threading
}


/**
 * #### Generated from [Example.kt:93]
 */
abstract class BaseClass (
    val baseField: Int
) : RdBindableBase() {
    //companion
    
    companion object : IAbstractDeclaration<BaseClass> {
        override fun readUnknownInstance(ctx: SerializationCtx, buffer: AbstractBuffer, unknownId: RdId, size: Int): BaseClass  {
            val objectStartPosition = buffer.position
            val _id = RdId.read(buffer)
            val baseField = buffer.readInt()
            val unknownBytes = ByteArray(objectStartPosition + size - buffer.position)
            buffer.readByteArrayRaw(unknownBytes)
            return BaseClass_Unknown(baseField, unknownId, unknownBytes).withId(_id)
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
    //threading
}


/**
 * #### Generated from [Example.kt:133]
 */
abstract class BaseClassWithInterface (
) : RdBindableBase(), Interface {
    //companion
    
    companion object : IAbstractDeclaration<BaseClassWithInterface> {
        override fun readUnknownInstance(ctx: SerializationCtx, buffer: AbstractBuffer, unknownId: RdId, size: Int): BaseClassWithInterface  {
            val objectStartPosition = buffer.position
            val _id = RdId.read(buffer)
            val unknownBytes = ByteArray(objectStartPosition + size - buffer.position)
            buffer.readByteArrayRaw(unknownBytes)
            return BaseClassWithInterface_Unknown(unknownId, unknownBytes).withId(_id)
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
    //threading
}


class BaseClassWithInterface_Unknown (
    override val unknownId: RdId,
    val unknownBytes: ByteArray
) : BaseClassWithInterface (
), IUnknownInstance {
    //write-marshaller
    private fun write(ctx: SerializationCtx, buffer: AbstractBuffer)  {
        rdid.write(buffer)
        buffer.writeByteArrayRaw(unknownBytes)
    }
    //companion
    
    companion object : IMarshaller<BaseClassWithInterface_Unknown> {
        override val _type: KClass<BaseClassWithInterface_Unknown> = BaseClassWithInterface_Unknown::class
        override val id: RdId get() = RdId(1299733233306533130)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): BaseClassWithInterface_Unknown  {
            throw NotImplementedError("Unknown instances should not be read via serializer")
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: BaseClassWithInterface_Unknown)  {
            value.write(ctx, buffer)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("BaseClassWithInterface_Unknown (")
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): BaseClassWithInterface_Unknown   {
        return BaseClassWithInterface_Unknown(
            unknownId,
            unknownBytes
        )
    }
    //contexts
    //threading
}


class BaseClass_Unknown (
    baseField: Int,
    override val unknownId: RdId,
    val unknownBytes: ByteArray
) : BaseClass (
    baseField
), IUnknownInstance {
    //write-marshaller
    private fun write(ctx: SerializationCtx, buffer: AbstractBuffer)  {
        rdid.write(buffer)
        buffer.writeInt(baseField)
        buffer.writeByteArrayRaw(unknownBytes)
    }
    //companion
    
    companion object : IMarshaller<BaseClass_Unknown> {
        override val _type: KClass<BaseClass_Unknown> = BaseClass_Unknown::class
        override val id: RdId get() = RdId(-8929432671501473473)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): BaseClass_Unknown  {
            throw NotImplementedError("Unknown instances should not be read via serializer")
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: BaseClass_Unknown)  {
            value.write(ctx, buffer)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("BaseClass_Unknown (")
        printer.indent {
            print("baseField = "); baseField.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): BaseClass_Unknown   {
        return BaseClass_Unknown(
            baseField,
            unknownId,
            unknownBytes
        )
    }
    //contexts
    //threading
}


/**
 * #### Generated from [Example.kt:97]
 */
abstract class BaseStruct (
    val baseField: Int
) : IPrintable {
    //companion
    
    companion object : IAbstractDeclaration<BaseStruct> {
        override fun readUnknownInstance(ctx: SerializationCtx, buffer: AbstractBuffer, unknownId: RdId, size: Int): BaseStruct  {
            val objectStartPosition = buffer.position
            val baseField = buffer.readInt()
            val unknownBytes = ByteArray(objectStartPosition + size - buffer.position)
            buffer.readByteArrayRaw(unknownBytes)
            return BaseStruct_Unknown(baseField, unknownId, unknownBytes)
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
    //threading
}


/**
 * #### Generated from [Example.kt:136]
 */
abstract class BaseStructWithInterface (
) : IPrintable, Interface {
    //companion
    
    companion object : IAbstractDeclaration<BaseStructWithInterface> {
        override fun readUnknownInstance(ctx: SerializationCtx, buffer: AbstractBuffer, unknownId: RdId, size: Int): BaseStructWithInterface  {
            val objectStartPosition = buffer.position
            val unknownBytes = ByteArray(objectStartPosition + size - buffer.position)
            buffer.readByteArrayRaw(unknownBytes)
            return BaseStructWithInterface_Unknown(unknownId, unknownBytes)
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
    //threading
}


class BaseStructWithInterface_Unknown (
    override val unknownId: RdId,
    val unknownBytes: ByteArray
) : BaseStructWithInterface (
), IUnknownInstance {
    //write-marshaller
    private fun write(ctx: SerializationCtx, buffer: AbstractBuffer)  {
        buffer.writeByteArrayRaw(unknownBytes)
    }
    //companion
    
    companion object : IMarshaller<BaseStructWithInterface_Unknown> {
        override val _type: KClass<BaseStructWithInterface_Unknown> = BaseStructWithInterface_Unknown::class
        override val id: RdId get() = RdId(-2819848318270682619)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): BaseStructWithInterface_Unknown  {
            throw NotImplementedError("Unknown instances should not be read via serializer")
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: BaseStructWithInterface_Unknown)  {
            value.write(ctx, buffer)
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
        
        other as BaseStructWithInterface_Unknown
        
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("BaseStructWithInterface_Unknown (")
        printer.print(")")
    }
    
    override fun toString() = PrettyPrinter().singleLine().also { print(it) }.toString()
    //deepClone
    //contexts
    //threading
}


class BaseStruct_Unknown (
    baseField: Int,
    override val unknownId: RdId,
    val unknownBytes: ByteArray
) : BaseStruct (
    baseField
), IUnknownInstance {
    //write-marshaller
    private fun write(ctx: SerializationCtx, buffer: AbstractBuffer)  {
        buffer.writeInt(baseField)
        buffer.writeByteArrayRaw(unknownBytes)
    }
    //companion
    
    companion object : IMarshaller<BaseStruct_Unknown> {
        override val _type: KClass<BaseStruct_Unknown> = BaseStruct_Unknown::class
        override val id: RdId get() = RdId(-8524399809491771292)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): BaseStruct_Unknown  {
            throw NotImplementedError("Unknown instances should not be read via serializer")
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: BaseStruct_Unknown)  {
            value.write(ctx, buffer)
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
        
        other as BaseStruct_Unknown
        
        if (baseField != other.baseField) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + baseField.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("BaseStruct_Unknown (")
        printer.indent {
            print("baseField = "); baseField.print(printer); println()
        }
        printer.print(")")
    }
    
    override fun toString() = PrettyPrinter().singleLine().also { print(it) }.toString()
    //deepClone
    //contexts
    //threading
}


/**
 * #### Generated from [Example.kt:52]
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
    private val _duration_prop: RdOptionalProperty<Duration>,
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
    //write-marshaller
    private fun write(ctx: SerializationCtx, buffer: AbstractBuffer)  {
        rdid.write(buffer)
        RdOptionalProperty.write(ctx, buffer, _y)
        RdOptionalProperty.write(ctx, buffer, _z)
        buffer.writeInt(x)
        RdMap.write(ctx, buffer, _sdf)
        buffer.writeList(foo) { v -> ctx.serializers.writePolymorphic(ctx, buffer, v) }
        buffer.writeList(bar) { v -> buffer.writeNullable(v) { ctx.serializers.writePolymorphic(ctx, buffer, it) } }
        buffer.writeString(nls_field)
        buffer.writeNullable(nls_nullable_field) { buffer.writeString(it) }
        buffer.writeList(string_list_field) { v -> buffer.writeString(v) }
        buffer.writeList(nls_list_field) { v -> buffer.writeString(v) }
        RdProperty.write(ctx, buffer, _foo1)
        RdProperty.write(ctx, buffer, _bar1)
        RdMap.write(ctx, buffer, _mapScalar)
        RdMap.write(ctx, buffer, _mapBindable)
        RdProperty.write(ctx, buffer, _property_with_default_nls)
        RdOptionalProperty.write(ctx, buffer, _property_with_several_attrs)
        RdOptionalProperty.write(ctx, buffer, _nls_prop)
        RdProperty.write(ctx, buffer, _nullable_nls_prop)
        buffer.writeString(non_nls_open_field)
        RdOptionalProperty.write(ctx, buffer, _duration_prop)
    }
    //companion
    
    companion object : IMarshaller<Baz> {
        override val _type: KClass<Baz> = Baz::class
        override val id: RdId get() = RdId(632584)
        
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
            val _duration_prop = RdOptionalProperty.read(ctx, buffer, FrameworkMarshallers.TimeSpan)
            return Baz(foo, bar, nls_field, nls_nullable_field, string_list_field, nls_list_field, _foo1, _bar1, _mapScalar, _mapBindable, _property_with_default_nls, _property_with_several_attrs, _nls_prop, _nullable_nls_prop, non_nls_open_field, _duration_prop, _y, _z, x, _sdf).withId(_id)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: Baz)  {
            value.write(ctx, buffer)
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
    val mapScalar: IMutableViewableMap<Int, ScalarPrimer> get() = _mapScalar
    val mapBindable: IMutableViewableMap<Int, FooBar> get() = _mapBindable
    val property_with_default_nls: IProperty<@org.jetbrains.annotations.Nls String> get() = _property_with_default_nls
    val property_with_several_attrs: IOptProperty<@org.jetbrains.annotations.Nls @org.jetbrains.annotations.NonNls String> get() = _property_with_several_attrs
    val nls_prop: IOptProperty<@org.jetbrains.annotations.Nls String> get() = _nls_prop
    val nullable_nls_prop: IProperty<@org.jetbrains.annotations.Nls String?> get() = _nullable_nls_prop
    val duration_prop: IOptProperty<Duration> get() = _duration_prop
    //methods
    //initializer
    init {
        _mapScalar.optimizeNested = true
        _property_with_default_nls.optimizeNested = true
        _property_with_several_attrs.optimizeNested = true
        _nls_prop.optimizeNested = true
        _nullable_nls_prop.optimizeNested = true
        _duration_prop.optimizeNested = true
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
        bindableChildren.add("duration_prop" to _duration_prop)
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
        RdOptionalProperty<Duration>(FrameworkMarshallers.TimeSpan),
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
            print("duration_prop = "); _duration_prop.print(printer); println()
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
            _duration_prop.deepClonePolymorphic(),
            _y.deepClonePolymorphic(),
            _z.deepClonePolymorphic(),
            x,
            _sdf.deepClonePolymorphic()
        )
    }
    //contexts
    //threading
}


/**
 * #### Generated from [Example.kt:87]
 */
class Class (
) : RdBindableBase() {
    //write-marshaller
    private fun write(ctx: SerializationCtx, buffer: AbstractBuffer)  {
        rdid.write(buffer)
    }
    //companion
    
    companion object : IMarshaller<Class> {
        override val _type: KClass<Class> = Class::class
        override val id: RdId get() = RdId(609144101)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): Class  {
            val _id = RdId.read(buffer)
            return Class().withId(_id)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: Class)  {
            value.write(ctx, buffer)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("Class (")
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): Class   {
        return Class(
        )
    }
    //contexts
    //threading
}


/**
 * #### Generated from [Example.kt:167]
 */
class Completion private constructor(
    private val _lookupItems: RdMap<Int, Boolean>
) : RdBindableBase() {
    //write-marshaller
    private fun write(ctx: SerializationCtx, buffer: AbstractBuffer)  {
        rdid.write(buffer)
        RdMap.write(ctx, buffer, _lookupItems)
    }
    //companion
    
    companion object : IMarshaller<Completion> {
        override val _type: KClass<Completion> = Completion::class
        override val id: RdId get() = RdId(17442164506690639)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): Completion  {
            val _id = RdId.read(buffer)
            val _lookupItems = RdMap.read(ctx, buffer, FrameworkMarshallers.Int, FrameworkMarshallers.Bool)
            return Completion(_lookupItems).withId(_id)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: Completion)  {
            value.write(ctx, buffer)
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
    //threading
}


/**
 * #### Generated from [Example.kt:125]
 */
abstract class DerivedBaseClass (
    val derivedField: Boolean,
    baseField: Int
) : BaseClass (
    baseField
) {
    //companion
    
    companion object : IAbstractDeclaration<DerivedBaseClass> {
        override fun readUnknownInstance(ctx: SerializationCtx, buffer: AbstractBuffer, unknownId: RdId, size: Int): DerivedBaseClass  {
            val objectStartPosition = buffer.position
            val _id = RdId.read(buffer)
            val derivedField = buffer.readBool()
            val baseField = buffer.readInt()
            val unknownBytes = ByteArray(objectStartPosition + size - buffer.position)
            buffer.readByteArrayRaw(unknownBytes)
            return DerivedBaseClass_Unknown(derivedField, baseField, unknownId, unknownBytes).withId(_id)
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
    //threading
}


class DerivedBaseClass_Unknown (
    derivedField: Boolean,
    baseField: Int,
    override val unknownId: RdId,
    val unknownBytes: ByteArray
) : DerivedBaseClass (
    derivedField,
    baseField
), IUnknownInstance {
    //write-marshaller
    private fun write(ctx: SerializationCtx, buffer: AbstractBuffer)  {
        rdid.write(buffer)
        buffer.writeBool(derivedField)
        buffer.writeInt(baseField)
        buffer.writeByteArrayRaw(unknownBytes)
    }
    //companion
    
    companion object : IMarshaller<DerivedBaseClass_Unknown> {
        override val _type: KClass<DerivedBaseClass_Unknown> = DerivedBaseClass_Unknown::class
        override val id: RdId get() = RdId(9208993593714803624)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): DerivedBaseClass_Unknown  {
            throw NotImplementedError("Unknown instances should not be read via serializer")
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: DerivedBaseClass_Unknown)  {
            value.write(ctx, buffer)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("DerivedBaseClass_Unknown (")
        printer.indent {
            print("derivedField = "); derivedField.print(printer); println()
            print("baseField = "); baseField.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): DerivedBaseClass_Unknown   {
        return DerivedBaseClass_Unknown(
            derivedField,
            baseField,
            unknownId,
            unknownBytes
        )
    }
    //contexts
    //threading
}


/**
 * #### Generated from [Example.kt:129]
 */
abstract class DerivedBaseStruct (
    val derivedField: Boolean,
    baseField: Int
) : BaseStruct (
    baseField
) {
    //companion
    
    companion object : IAbstractDeclaration<DerivedBaseStruct> {
        override fun readUnknownInstance(ctx: SerializationCtx, buffer: AbstractBuffer, unknownId: RdId, size: Int): DerivedBaseStruct  {
            val objectStartPosition = buffer.position
            val derivedField = buffer.readBool()
            val baseField = buffer.readInt()
            val unknownBytes = ByteArray(objectStartPosition + size - buffer.position)
            buffer.readByteArrayRaw(unknownBytes)
            return DerivedBaseStruct_Unknown(derivedField, baseField, unknownId, unknownBytes)
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
    //threading
}


class DerivedBaseStruct_Unknown (
    derivedField: Boolean,
    baseField: Int,
    override val unknownId: RdId,
    val unknownBytes: ByteArray
) : DerivedBaseStruct (
    derivedField,
    baseField
), IUnknownInstance {
    //write-marshaller
    private fun write(ctx: SerializationCtx, buffer: AbstractBuffer)  {
        buffer.writeBool(derivedField)
        buffer.writeInt(baseField)
        buffer.writeByteArrayRaw(unknownBytes)
    }
    //companion
    
    companion object : IMarshaller<DerivedBaseStruct_Unknown> {
        override val _type: KClass<DerivedBaseStruct_Unknown> = DerivedBaseStruct_Unknown::class
        override val id: RdId get() = RdId(364492200926270235)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): DerivedBaseStruct_Unknown  {
            throw NotImplementedError("Unknown instances should not be read via serializer")
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: DerivedBaseStruct_Unknown)  {
            value.write(ctx, buffer)
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
        
        other as DerivedBaseStruct_Unknown
        
        if (derivedField != other.derivedField) return false
        if (baseField != other.baseField) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + derivedField.hashCode()
        __r = __r*31 + baseField.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("DerivedBaseStruct_Unknown (")
        printer.indent {
            print("derivedField = "); derivedField.print(printer); println()
            print("baseField = "); baseField.print(printer); println()
        }
        printer.print(")")
    }
    
    override fun toString() = PrettyPrinter().singleLine().also { print(it) }.toString()
    //deepClone
    //contexts
    //threading
}


/**
 * #### Generated from [Example.kt:109]
 */
class DerivedClass (
    val derivedField: Boolean,
    baseField: Int
) : BaseClass (
    baseField
) {
    //write-marshaller
    private fun write(ctx: SerializationCtx, buffer: AbstractBuffer)  {
        rdid.write(buffer)
        buffer.writeInt(baseField)
        buffer.writeBool(derivedField)
    }
    //companion
    
    companion object : IMarshaller<DerivedClass> {
        override val _type: KClass<DerivedClass> = DerivedClass::class
        override val id: RdId get() = RdId(-1667485286246826738)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): DerivedClass  {
            val _id = RdId.read(buffer)
            val baseField = buffer.readInt()
            val derivedField = buffer.readBool()
            return DerivedClass(derivedField, baseField).withId(_id)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: DerivedClass)  {
            value.write(ctx, buffer)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("DerivedClass (")
        printer.indent {
            print("derivedField = "); derivedField.print(printer); println()
            print("baseField = "); baseField.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): DerivedClass   {
        return DerivedClass(
            derivedField,
            baseField
        )
    }
    //contexts
    //threading
}


/**
 * #### Generated from [Example.kt:139]
 */
abstract class DerivedClassWith2Interfaces (
    val derivedField: Boolean,
    baseField: Int
) : BaseClass (
    baseField
), Interface, Interface2 {
    //companion
    
    companion object : IAbstractDeclaration<DerivedClassWith2Interfaces> {
        override fun readUnknownInstance(ctx: SerializationCtx, buffer: AbstractBuffer, unknownId: RdId, size: Int): DerivedClassWith2Interfaces  {
            val objectStartPosition = buffer.position
            val _id = RdId.read(buffer)
            val derivedField = buffer.readBool()
            val baseField = buffer.readInt()
            val unknownBytes = ByteArray(objectStartPosition + size - buffer.position)
            buffer.readByteArrayRaw(unknownBytes)
            return DerivedClassWith2Interfaces_Unknown(derivedField, baseField, unknownId, unknownBytes).withId(_id)
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
    //threading
}


class DerivedClassWith2Interfaces_Unknown (
    derivedField: Boolean,
    baseField: Int,
    override val unknownId: RdId,
    val unknownBytes: ByteArray
) : DerivedClassWith2Interfaces (
    derivedField,
    baseField
), IUnknownInstance {
    //write-marshaller
    private fun write(ctx: SerializationCtx, buffer: AbstractBuffer)  {
        rdid.write(buffer)
        buffer.writeBool(derivedField)
        buffer.writeInt(baseField)
        buffer.writeByteArrayRaw(unknownBytes)
    }
    //companion
    
    companion object : IMarshaller<DerivedClassWith2Interfaces_Unknown> {
        override val _type: KClass<DerivedClassWith2Interfaces_Unknown> = DerivedClassWith2Interfaces_Unknown::class
        override val id: RdId get() = RdId(-485232411962447965)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): DerivedClassWith2Interfaces_Unknown  {
            throw NotImplementedError("Unknown instances should not be read via serializer")
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: DerivedClassWith2Interfaces_Unknown)  {
            value.write(ctx, buffer)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("DerivedClassWith2Interfaces_Unknown (")
        printer.indent {
            print("derivedField = "); derivedField.print(printer); println()
            print("baseField = "); baseField.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): DerivedClassWith2Interfaces_Unknown   {
        return DerivedClassWith2Interfaces_Unknown(
            derivedField,
            baseField,
            unknownId,
            unknownBytes
        )
    }
    //contexts
    //threading
}


/**
 * #### Generated from [Example.kt:117]
 */
open class DerivedOpenClass (
    val derivedField: Boolean,
    baseField: Int
) : OpenClass (
    baseField
) {
    //write-marshaller
    private fun write(ctx: SerializationCtx, buffer: AbstractBuffer)  {
        rdid.write(buffer)
        buffer.writeInt(baseField)
        buffer.writeBool(derivedField)
    }
    //companion
    
    companion object : IMarshaller<DerivedOpenClass>, IAbstractDeclaration<DerivedOpenClass> {
        override val _type: KClass<DerivedOpenClass> = DerivedOpenClass::class
        override val id: RdId get() = RdId(-5037012260488689180)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): DerivedOpenClass  {
            val _id = RdId.read(buffer)
            val baseField = buffer.readInt()
            val derivedField = buffer.readBool()
            return DerivedOpenClass(derivedField, baseField).withId(_id)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: DerivedOpenClass)  {
            value.write(ctx, buffer)
        }
        
        
        override fun readUnknownInstance(ctx: SerializationCtx, buffer: AbstractBuffer, unknownId: RdId, size: Int): DerivedOpenClass  {
            val objectStartPosition = buffer.position
            val _id = RdId.read(buffer)
            val derivedField = buffer.readBool()
            val baseField = buffer.readInt()
            val unknownBytes = ByteArray(objectStartPosition + size - buffer.position)
            buffer.readByteArrayRaw(unknownBytes)
            return DerivedOpenClass_Unknown(derivedField, baseField, unknownId, unknownBytes).withId(_id)
        }
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("DerivedOpenClass (")
        printer.indent {
            print("derivedField = "); derivedField.print(printer); println()
            print("baseField = "); baseField.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): DerivedOpenClass   {
        return DerivedOpenClass(
            derivedField,
            baseField
        )
    }
    //contexts
    //threading
}


class DerivedOpenClass_Unknown (
    derivedField: Boolean,
    baseField: Int,
    override val unknownId: RdId,
    val unknownBytes: ByteArray
) : DerivedOpenClass (
    derivedField,
    baseField
), IUnknownInstance {
    //write-marshaller
    private fun write(ctx: SerializationCtx, buffer: AbstractBuffer)  {
        rdid.write(buffer)
        buffer.writeBool(derivedField)
        buffer.writeInt(baseField)
        buffer.writeByteArrayRaw(unknownBytes)
    }
    //companion
    
    companion object : IMarshaller<DerivedOpenClass_Unknown> {
        override val _type: KClass<DerivedOpenClass_Unknown> = DerivedOpenClass_Unknown::class
        override val id: RdId get() = RdId(-5977551086017277745)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): DerivedOpenClass_Unknown  {
            throw NotImplementedError("Unknown instances should not be read via serializer")
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: DerivedOpenClass_Unknown)  {
            value.write(ctx, buffer)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("DerivedOpenClass_Unknown (")
        printer.indent {
            print("derivedField = "); derivedField.print(printer); println()
            print("baseField = "); baseField.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): DerivedOpenClass_Unknown   {
        return DerivedOpenClass_Unknown(
            derivedField,
            baseField,
            unknownId,
            unknownBytes
        )
    }
    //contexts
    //threading
}


/**
 * #### Generated from [Example.kt:121]
 */
open class DerivedOpenStruct (
    val derivedField: Boolean,
    baseField: Int
) : OpenStruct (
    baseField
) {
    //write-marshaller
    private fun write(ctx: SerializationCtx, buffer: AbstractBuffer)  {
        buffer.writeInt(baseField)
        buffer.writeBool(derivedField)
    }
    //companion
    
    companion object : IMarshaller<DerivedOpenStruct>, IAbstractDeclaration<DerivedOpenStruct> {
        override val _type: KClass<DerivedOpenStruct> = DerivedOpenStruct::class
        override val id: RdId get() = RdId(-8573427485006989079)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): DerivedOpenStruct  {
            val baseField = buffer.readInt()
            val derivedField = buffer.readBool()
            return DerivedOpenStruct(derivedField, baseField)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: DerivedOpenStruct)  {
            value.write(ctx, buffer)
        }
        
        
        override fun readUnknownInstance(ctx: SerializationCtx, buffer: AbstractBuffer, unknownId: RdId, size: Int): DerivedOpenStruct  {
            val objectStartPosition = buffer.position
            val derivedField = buffer.readBool()
            val baseField = buffer.readInt()
            val unknownBytes = ByteArray(objectStartPosition + size - buffer.position)
            buffer.readByteArrayRaw(unknownBytes)
            return DerivedOpenStruct_Unknown(derivedField, baseField, unknownId, unknownBytes)
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
        
        other as DerivedOpenStruct
        
        if (derivedField != other.derivedField) return false
        if (baseField != other.baseField) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + derivedField.hashCode()
        __r = __r*31 + baseField.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("DerivedOpenStruct (")
        printer.indent {
            print("derivedField = "); derivedField.print(printer); println()
            print("baseField = "); baseField.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}


class DerivedOpenStruct_Unknown (
    derivedField: Boolean,
    baseField: Int,
    override val unknownId: RdId,
    val unknownBytes: ByteArray
) : DerivedOpenStruct (
    derivedField,
    baseField
), IUnknownInstance {
    //write-marshaller
    private fun write(ctx: SerializationCtx, buffer: AbstractBuffer)  {
        buffer.writeBool(derivedField)
        buffer.writeInt(baseField)
        buffer.writeByteArrayRaw(unknownBytes)
    }
    //companion
    
    companion object : IMarshaller<DerivedOpenStruct_Unknown> {
        override val _type: KClass<DerivedOpenStruct_Unknown> = DerivedOpenStruct_Unknown::class
        override val id: RdId get() = RdId(9196953045680089812)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): DerivedOpenStruct_Unknown  {
            throw NotImplementedError("Unknown instances should not be read via serializer")
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: DerivedOpenStruct_Unknown)  {
            value.write(ctx, buffer)
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
        
        other as DerivedOpenStruct_Unknown
        
        if (derivedField != other.derivedField) return false
        if (baseField != other.baseField) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + derivedField.hashCode()
        __r = __r*31 + baseField.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("DerivedOpenStruct_Unknown (")
        printer.indent {
            print("derivedField = "); derivedField.print(printer); println()
            print("baseField = "); baseField.print(printer); println()
        }
        printer.print(")")
    }
    
    override fun toString() = PrettyPrinter().singleLine().also { print(it) }.toString()
    //deepClone
    //contexts
    //threading
}


/**
 * #### Generated from [Example.kt:113]
 */
class DerivedStruct (
    val derivedField: Boolean,
    baseField: Int
) : BaseStruct (
    baseField
) {
    //write-marshaller
    private fun write(ctx: SerializationCtx, buffer: AbstractBuffer)  {
        buffer.writeInt(baseField)
        buffer.writeBool(derivedField)
    }
    //companion
    
    companion object : IMarshaller<DerivedStruct> {
        override val _type: KClass<DerivedStruct> = DerivedStruct::class
        override val id: RdId get() = RdId(3648188347942988543)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): DerivedStruct  {
            val baseField = buffer.readInt()
            val derivedField = buffer.readBool()
            return DerivedStruct(derivedField, baseField)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: DerivedStruct)  {
            value.write(ctx, buffer)
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
        
        other as DerivedStruct
        
        if (derivedField != other.derivedField) return false
        if (baseField != other.baseField) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + derivedField.hashCode()
        __r = __r*31 + baseField.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("DerivedStruct (")
        printer.indent {
            print("derivedField = "); derivedField.print(printer); println()
            print("baseField = "); baseField.print(printer); println()
        }
        printer.print(")")
    }
    
    override fun toString() = PrettyPrinter().singleLine().also { print(it) }.toString()
    //deepClone
    //contexts
    //threading
}


/**
 * #### Generated from [Example.kt:143]
 */
class DerivedStructWith2Interfaces (
    val derivedField: Boolean,
    baseField: Int
) : BaseStruct (
    baseField
), Interface, Interface2 {
    //write-marshaller
    private fun write(ctx: SerializationCtx, buffer: AbstractBuffer)  {
        buffer.writeInt(baseField)
        buffer.writeBool(derivedField)
    }
    //companion
    
    companion object : IMarshaller<DerivedStructWith2Interfaces> {
        override val _type: KClass<DerivedStructWith2Interfaces> = DerivedStructWith2Interfaces::class
        override val id: RdId get() = RdId(4287876202302424743)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): DerivedStructWith2Interfaces  {
            val baseField = buffer.readInt()
            val derivedField = buffer.readBool()
            return DerivedStructWith2Interfaces(derivedField, baseField)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: DerivedStructWith2Interfaces)  {
            value.write(ctx, buffer)
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
        
        other as DerivedStructWith2Interfaces
        
        if (derivedField != other.derivedField) return false
        if (baseField != other.baseField) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + derivedField.hashCode()
        __r = __r*31 + baseField.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("DerivedStructWith2Interfaces (")
        printer.indent {
            print("derivedField = "); derivedField.print(printer); println()
            print("baseField = "); baseField.print(printer); println()
        }
        printer.print(")")
    }
    
    override fun toString() = PrettyPrinter().singleLine().also { print(it) }.toString()
    //deepClone
    //contexts
    //threading
}


/**
 * #### Generated from [Example.kt:162]
 */
class Document private constructor(
    val moniker: FooBar,
    val buffer: String?,
    private val _andBackAgain: RdCall<String, Int>,
    val completion: Completion,
    val arr1: ByteArray,
    val arr2: Array<BooleanArray>
) : RdBindableBase() {
    //write-marshaller
    private fun write(ctx: SerializationCtx, buffer: AbstractBuffer)  {
        rdid.write(buffer)
        FooBar.write(ctx, buffer, moniker)
        buffer.writeNullable(buffer) { buffer.writeString(it) }
        RdCall.write(ctx, buffer, _andBackAgain)
        Completion.write(ctx, buffer, completion)
        buffer.writeByteArray(arr1)
        buffer.writeArray(arr2) { buffer.writeBooleanArray(it) }
    }
    //companion
    
    companion object : IMarshaller<Document> {
        override val _type: KClass<Document> = Document::class
        override val id: RdId get() = RdId(18177246065230)
        
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
            value.write(ctx, buffer)
        }
        
        
    }
    //fields
    val andBackAgain: IRdCall<String, Int> get() = _andBackAgain
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
    //threading
}


/**
 * #### Generated from [Example.kt:27]
 */
enum class EnumSetTest {
    a, 
    b, 
    c;
    
    companion object : IMarshaller<EnumSet<EnumSetTest>> {
        val marshaller = FrameworkMarshallers.enumSet<EnumSetTest>()
        
        
        override val _type: KClass<EnumSetTest> = EnumSetTest::class
        override val id: RdId get() = RdId(542326635061440960)
        
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): EnumSet<EnumSetTest> {
            return marshaller.read(ctx, buffer)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: EnumSet<EnumSetTest>)  {
            marshaller.write(ctx, buffer, value)
        }
    }
}


/**
 * #### Generated from [Example.kt:35]
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
    //threading
}


/**
 * #### Generated from [Example.kt:76]
 */
class FooBar (
    val a: Baz
) : RdBindableBase() {
    //write-marshaller
    private fun write(ctx: SerializationCtx, buffer: AbstractBuffer)  {
        rdid.write(buffer)
        Baz.write(ctx, buffer, a)
    }
    //companion
    
    companion object : IMarshaller<FooBar> {
        override val _type: KClass<FooBar> = FooBar::class
        override val id: RdId get() = RdId(18972494688)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): FooBar  {
            val _id = RdId.read(buffer)
            val a = Baz.read(ctx, buffer)
            return FooBar(a).withId(_id)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: FooBar)  {
            value.write(ctx, buffer)
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
    //threading
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
    //write-marshaller
    private fun write(ctx: SerializationCtx, buffer: AbstractBuffer)  {
        rdid.write(buffer)
        buffer.writeInt(x)
        RdMap.write(ctx, buffer, _sdf)
        buffer.writeByteArrayRaw(unknownBytes)
    }
    //companion
    
    companion object : IMarshaller<Foo_Unknown> {
        override val _type: KClass<Foo_Unknown> = Foo_Unknown::class
        override val id: RdId get() = RdId(543167202472902558)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): Foo_Unknown  {
            throw NotImplementedError("Unknown instances should not be read via serializer")
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: Foo_Unknown)  {
            value.write(ctx, buffer)
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
    //threading
}


/**
 * #### Generated from [Example.kt:81]
 */
interface Interface
{
}

/**
 * #### Generated from [Example.kt:84]
 */
interface Interface2
{
}

/**
 * #### Generated from [Example.kt:101]
 */
open class OpenClass (
    val baseField: Int
) : RdBindableBase() {
    //write-marshaller
    private fun write(ctx: SerializationCtx, buffer: AbstractBuffer)  {
        rdid.write(buffer)
        buffer.writeInt(baseField)
    }
    //companion
    
    companion object : IMarshaller<OpenClass>, IAbstractDeclaration<OpenClass> {
        override val _type: KClass<OpenClass> = OpenClass::class
        override val id: RdId get() = RdId(572905478059643)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): OpenClass  {
            val _id = RdId.read(buffer)
            val baseField = buffer.readInt()
            return OpenClass(baseField).withId(_id)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: OpenClass)  {
            value.write(ctx, buffer)
        }
        
        
        override fun readUnknownInstance(ctx: SerializationCtx, buffer: AbstractBuffer, unknownId: RdId, size: Int): OpenClass  {
            val objectStartPosition = buffer.position
            val _id = RdId.read(buffer)
            val baseField = buffer.readInt()
            val unknownBytes = ByteArray(objectStartPosition + size - buffer.position)
            buffer.readByteArrayRaw(unknownBytes)
            return OpenClass_Unknown(baseField, unknownId, unknownBytes).withId(_id)
        }
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("OpenClass (")
        printer.indent {
            print("baseField = "); baseField.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): OpenClass   {
        return OpenClass(
            baseField
        )
    }
    //contexts
    //threading
}


class OpenClass_Unknown (
    baseField: Int,
    override val unknownId: RdId,
    val unknownBytes: ByteArray
) : OpenClass (
    baseField
), IUnknownInstance {
    //write-marshaller
    private fun write(ctx: SerializationCtx, buffer: AbstractBuffer)  {
        rdid.write(buffer)
        buffer.writeInt(baseField)
        buffer.writeByteArrayRaw(unknownBytes)
    }
    //companion
    
    companion object : IMarshaller<OpenClass_Unknown> {
        override val _type: KClass<OpenClass_Unknown> = OpenClass_Unknown::class
        override val id: RdId get() = RdId(-5669233277524003226)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): OpenClass_Unknown  {
            throw NotImplementedError("Unknown instances should not be read via serializer")
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: OpenClass_Unknown)  {
            value.write(ctx, buffer)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("OpenClass_Unknown (")
        printer.indent {
            print("baseField = "); baseField.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): OpenClass_Unknown   {
        return OpenClass_Unknown(
            baseField,
            unknownId,
            unknownBytes
        )
    }
    //contexts
    //threading
}


/**
 * #### Generated from [Example.kt:105]
 */
open class OpenStruct (
    val baseField: Int
) : IPrintable {
    //write-marshaller
    private fun write(ctx: SerializationCtx, buffer: AbstractBuffer)  {
        buffer.writeInt(baseField)
    }
    //companion
    
    companion object : IMarshaller<OpenStruct>, IAbstractDeclaration<OpenStruct> {
        override val _type: KClass<OpenStruct> = OpenStruct::class
        override val id: RdId get() = RdId(17760070285811506)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): OpenStruct  {
            val baseField = buffer.readInt()
            return OpenStruct(baseField)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: OpenStruct)  {
            value.write(ctx, buffer)
        }
        
        
        override fun readUnknownInstance(ctx: SerializationCtx, buffer: AbstractBuffer, unknownId: RdId, size: Int): OpenStruct  {
            val objectStartPosition = buffer.position
            val baseField = buffer.readInt()
            val unknownBytes = ByteArray(objectStartPosition + size - buffer.position)
            buffer.readByteArrayRaw(unknownBytes)
            return OpenStruct_Unknown(baseField, unknownId, unknownBytes)
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
        
        other as OpenStruct
        
        if (baseField != other.baseField) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + baseField.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("OpenStruct (")
        printer.indent {
            print("baseField = "); baseField.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}


class OpenStruct_Unknown (
    baseField: Int,
    override val unknownId: RdId,
    val unknownBytes: ByteArray
) : OpenStruct (
    baseField
), IUnknownInstance {
    //write-marshaller
    private fun write(ctx: SerializationCtx, buffer: AbstractBuffer)  {
        buffer.writeInt(baseField)
        buffer.writeByteArrayRaw(unknownBytes)
    }
    //companion
    
    companion object : IMarshaller<OpenStruct_Unknown> {
        override val _type: KClass<OpenStruct_Unknown> = OpenStruct_Unknown::class
        override val id: RdId get() = RdId(308061035262048285)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): OpenStruct_Unknown  {
            throw NotImplementedError("Unknown instances should not be read via serializer")
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: OpenStruct_Unknown)  {
            value.write(ctx, buffer)
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
        
        other as OpenStruct_Unknown
        
        if (baseField != other.baseField) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + baseField.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("OpenStruct_Unknown (")
        printer.indent {
            print("baseField = "); baseField.print(printer); println()
        }
        printer.print(")")
    }
    
    override fun toString() = PrettyPrinter().singleLine().also { print(it) }.toString()
    //deepClone
    //contexts
    //threading
}


/**
 * #### Generated from [Example.kt:176]
 */
data class ScalarExample (
    val intfield: Int
) : IPrintable {
    //write-marshaller
    private fun write(ctx: SerializationCtx, buffer: AbstractBuffer)  {
        buffer.writeInt(intfield)
    }
    //companion
    
    companion object : IMarshaller<ScalarExample> {
        override val _type: KClass<ScalarExample> = ScalarExample::class
        override val id: RdId get() = RdId(-3048302864262156661)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): ScalarExample  {
            val intfield = buffer.readInt()
            return ScalarExample(intfield)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: ScalarExample)  {
            value.write(ctx, buffer)
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
    //threading
}


/**
 * #### Generated from [Example.kt:40]
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
    //threading
}


class ScalarPrimer_Unknown (
    x: Int,
    override val unknownId: RdId,
    val unknownBytes: ByteArray
) : ScalarPrimer (
    x
), IUnknownInstance {
    //write-marshaller
    private fun write(ctx: SerializationCtx, buffer: AbstractBuffer)  {
        buffer.writeInt(x)
        buffer.writeByteArrayRaw(unknownBytes)
    }
    //companion
    
    companion object : IMarshaller<ScalarPrimer_Unknown> {
        override val _type: KClass<ScalarPrimer_Unknown> = ScalarPrimer_Unknown::class
        override val id: RdId get() = RdId(-427415512200834691)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): ScalarPrimer_Unknown  {
            throw NotImplementedError("Unknown instances should not be read via serializer")
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: ScalarPrimer_Unknown)  {
            value.write(ctx, buffer)
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
    //threading
}


/**
 * #### Generated from [Example.kt:23]
 */
data class Selection (
    val start: Int,
    val end: Int,
    val lst: IntArray,
    val enumSetTest: EnumSet<EnumSetTest>,
    val nls_field: @org.jetbrains.annotations.Nls String
) : IPrintable {
    //write-marshaller
    private fun write(ctx: SerializationCtx, buffer: AbstractBuffer)  {
        buffer.writeInt(start)
        buffer.writeInt(end)
        buffer.writeIntArray(lst)
        buffer.writeEnumSet(enumSetTest)
        buffer.writeString(nls_field)
    }
    //companion
    
    companion object : IMarshaller<Selection> {
        override val _type: KClass<Selection> = Selection::class
        override val id: RdId get() = RdId(576020388116153)
        
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
            value.write(ctx, buffer)
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
    //threading
}


/**
 * #### Generated from [Example.kt:90]
 */
class Struct (
) : IPrintable {
    //write-marshaller
    private fun write(ctx: SerializationCtx, buffer: AbstractBuffer)  {
    }
    //companion
    
    companion object : IMarshaller<Struct> {
        override val _type: KClass<Struct> = Struct::class
        override val id: RdId get() = RdId(19349429704)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): Struct  {
            return Struct()
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: Struct)  {
            value.write(ctx, buffer)
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
        
        other as Struct
        
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("Struct (")
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}


/**
 * #### Generated from [Example.kt:179]
 */
class TextControl private constructor(
    private val _selection: RdOptionalProperty<Selection>,
    private val _vsink: RdSignal<Unit>,
    private val _vsource: RdSignal<Unit>,
    private val _there1: RdCall<Int, String>
) : RdBindableBase() {
    //write-marshaller
    private fun write(ctx: SerializationCtx, buffer: AbstractBuffer)  {
        rdid.write(buffer)
        RdOptionalProperty.write(ctx, buffer, _selection)
        RdSignal.write(ctx, buffer, _vsink)
        RdSignal.write(ctx, buffer, _vsource)
        RdCall.write(ctx, buffer, _there1)
    }
    //companion
    
    companion object : IMarshaller<TextControl> {
        override val _type: KClass<TextControl> = TextControl::class
        override val id: RdId get() = RdId(554385840109775197)
        
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
            value.write(ctx, buffer)
        }
        
        
    }
    //fields
    val selection: IOptProperty<Selection> get() = _selection
    val vsink: ISignal<Unit> get() = _vsink
    val vsource: ISource<Unit> get() = _vsource
    val there1: IRdEndpoint<Int, String> get() = _there1
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
    //threading
}


/**
 * #### Generated from [Example.kt:149]
 */
data class UseStructTest (
    val testField: Int,
    val testField2: String
) : IPrintable {
    //write-marshaller
    private fun write(ctx: SerializationCtx, buffer: AbstractBuffer)  {
        buffer.writeInt(testField)
        buffer.writeString(testField2)
    }
    //companion
    
    companion object : IMarshaller<UseStructTest> {
        override val _type: KClass<UseStructTest> = UseStructTest::class
        override val id: RdId get() = RdId(-1063807896803205733)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): UseStructTest  {
            val testField = buffer.readInt()
            val testField2 = buffer.readString()
            return UseStructTest(testField, testField2)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: UseStructTest)  {
            value.write(ctx, buffer)
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
        
        other as UseStructTest
        
        if (testField != other.testField) return false
        if (testField2 != other.testField2) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + testField.hashCode()
        __r = __r*31 + testField2.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("UseStructTest (")
        printer.indent {
            print("testField = "); testField.print(printer); println()
            print("testField2 = "); testField2.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}


/**
 * #### Generated from [Example.kt:147]
 */
@kotlin.jvm.JvmInline value class ValueStruct (
    val value: Int
) : IPrintable {
    //write-marshaller
    private fun write(ctx: SerializationCtx, buffer: AbstractBuffer)  {
        buffer.writeInt(value)
    }
    //companion
    
    companion object : IMarshaller<ValueStruct> {
        override val _type: KClass<ValueStruct> = ValueStruct::class
        override val id: RdId get() = RdId(555909160394251923)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): ValueStruct  {
            val value = buffer.readInt()
            return ValueStruct(value)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: ValueStruct)  {
            value.write(ctx, buffer)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("ValueStruct (")
        printer.indent {
            print("value = "); value.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}


/**
 * #### Generated from [Example.kt:46]
 */
enum class Z {
    Bar, 
    z1;
    
    companion object : IMarshaller<Z> {
        val marshaller = FrameworkMarshallers.enum<Z>()
        
        
        override val _type: KClass<Z> = Z::class
        override val id: RdId get() = RdId(679)
        
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): Z {
            return marshaller.read(ctx, buffer)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: Z)  {
            marshaller.write(ctx, buffer, value)
        }
    }
}
