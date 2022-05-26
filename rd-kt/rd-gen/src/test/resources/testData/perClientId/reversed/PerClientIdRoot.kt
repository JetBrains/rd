@file:Suppress("EXPERIMENTAL_API_USAGE","EXPERIMENTAL_UNSIGNED_LITERALS","PackageDirectoryMismatch","UnusedImport","unused","LocalVariableName","CanBeVal","PropertyName","EnumEntryName","ClassName","ObjectPropertyName","UnnecessaryVariable","SpellCheckingInspection")
package com.jetbrains.rd.framework.test.cases.perClientId

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
 * #### Generated from [PerClientId.kt:9]
 */
class PerClientIdRoot private constructor(
    private val _aProp: RdPerContextMap<String, RdOptionalProperty<String>>,
    private val _aPropDefault: RdPerContextMap<String, RdProperty<Boolean>>,
    private val _aPropDefault2: RdProperty<Boolean>,
    private val _aMap: RdPerContextMap<String, RdMap<String, String>>,
    private val _innerProp: RdOptionalProperty<InnerClass>
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            serializers.register(InnerClass)
            serializers.register(PerClientIdStruct)
            serializers.register(PerClientIdSignal)
            PerClientIdRoot.register(serializers)
        }
        
        
        
        
        
        const val serializationHash = 3214444051594582608L
        
    }
    override val serializersOwner: ISerializersOwner get() = PerClientIdRoot
    override val serializationHash: Long get() = PerClientIdRoot.serializationHash
    
    //fields
    val aProp: IOptProperty<String> get() = _aProp.getForCurrentContext()
    val aPropPerContextMap: IPerContextMap<String, IOptProperty<String>> get() = _aProp
    val aPropDefault: IProperty<Boolean> get() = _aPropDefault.getForCurrentContext()
    val aPropDefaultPerContextMap: IPerContextMap<String, IProperty<Boolean>> get() = _aPropDefault
    val aPropDefault2: IProperty<Boolean> get() = _aPropDefault2
    val aMap: IMutableViewableMap<String, String> get() = _aMap.getForCurrentContext()
    val aMapPerContextMap: IPerContextMap<String, IMutableViewableMap<String, String>> get() = _aMap
    val innerProp: IOptProperty<InnerClass> get() = _innerProp
    //methods
    //initializer
    init {
        _aPropDefault2.optimizeNested = true
    }
    
    init {
        bindableChildren.add("aProp" to _aProp)
        bindableChildren.add("aPropDefault" to _aPropDefault)
        bindableChildren.add("aPropDefault2" to _aPropDefault2)
        bindableChildren.add("aMap" to _aMap)
        bindableChildren.add("innerProp" to _innerProp)
    }
    
    //secondary constructor
    private constructor(
    ) : this(
        RdPerContextMap<String, RdOptionalProperty<String>>(PerClientIdRoot.Key, { RdOptionalProperty(FrameworkMarshallers.String) }),
        RdPerContextMap<String, RdProperty<Boolean>>(PerClientIdRoot.Key, { RdProperty(false, FrameworkMarshallers.Bool) }),
        RdProperty<Boolean>(true, FrameworkMarshallers.Bool),
        RdPerContextMap<String, RdMap<String, String>>(PerClientIdRoot.Key, { RdMap(FrameworkMarshallers.String, FrameworkMarshallers.String).apply { master = it } }),
        RdOptionalProperty<InnerClass>(InnerClass)
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("PerClientIdRoot (")
        printer.indent {
            print("aProp = "); _aProp.print(printer); println()
            print("aPropDefault = "); _aPropDefault.print(printer); println()
            print("aPropDefault2 = "); _aPropDefault2.print(printer); println()
            print("aMap = "); _aMap.print(printer); println()
            print("innerProp = "); _innerProp.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): PerClientIdRoot   {
        return PerClientIdRoot(
            _aProp.deepClonePolymorphic(),
            _aPropDefault.deepClonePolymorphic(),
            _aPropDefault2.deepClonePolymorphic(),
            _aMap.deepClonePolymorphic(),
            _innerProp.deepClonePolymorphic()
        )
    }
    //contexts
    object Key: ThreadLocalRdContext<String>("Key", true, FrameworkMarshallers.String)
    object LightKey: ThreadLocalRdContext<Int>("LightKey", false, FrameworkMarshallers.Int)
}


/**
 * #### Generated from [PerClientId.kt:21]
 */
class InnerClass private constructor(
    private val _someValue: RdPerContextMap<String, RdProperty<String?>>,
    private val _someClassValue: RdPerContextMap<String, RdOptionalProperty<PerClientIdStruct>>,
    private val _someClassSignal: RdPerContextMap<String, RdSignal<PerClientIdSignal>>
) : RdBindableBase() {
    //companion
    
    companion object : IMarshaller<InnerClass> {
        override val _type: KClass<InnerClass> = InnerClass::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): InnerClass  {
            val _id = RdId.read(buffer)
            val _someValue = RdPerContextMap.read(PerClientIdRoot.Key, buffer) { RdProperty(null, __StringNullableSerializer) }
            val _someClassValue = RdPerContextMap.read(PerClientIdRoot.Key, buffer) { RdOptionalProperty(PerClientIdStruct) }
            val _someClassSignal = RdPerContextMap.read(PerClientIdRoot.Key, buffer) { RdSignal(PerClientIdSignal) }
            return InnerClass(_someValue, _someClassValue, _someClassSignal).withId(_id)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: InnerClass)  {
            value.rdid.write(buffer)
            RdPerContextMap.write(buffer, value._someValue)
            RdPerContextMap.write(buffer, value._someClassValue)
            RdPerContextMap.write(buffer, value._someClassSignal)
        }
        
        private val __StringNullableSerializer = FrameworkMarshallers.String.nullable()
        
    }
    //fields
    val someValue: IProperty<String?> get() = _someValue.getForCurrentContext()
    val someValuePerContextMap: IPerContextMap<String, IProperty<String?>> get() = _someValue
    val someClassValue: IOptProperty<PerClientIdStruct> get() = _someClassValue.getForCurrentContext()
    val someClassValuePerContextMap: IPerContextMap<String, IOptProperty<PerClientIdStruct>> get() = _someClassValue
    val someClassSignal: ISignal<PerClientIdSignal> get() = _someClassSignal.getForCurrentContext()
    val someClassSignalPerContextMap: IPerContextMap<String, ISignal<PerClientIdSignal>> get() = _someClassSignal
    //methods
    //initializer
    init {
        bindableChildren.add("someValue" to _someValue)
        bindableChildren.add("someClassValue" to _someClassValue)
        bindableChildren.add("someClassSignal" to _someClassSignal)
    }
    
    //secondary constructor
    constructor(
    ) : this(
        RdPerContextMap<String, RdProperty<String?>>(PerClientIdRoot.Key, { RdProperty(null, __StringNullableSerializer) }),
        RdPerContextMap<String, RdOptionalProperty<PerClientIdStruct>>(PerClientIdRoot.Key, { RdOptionalProperty(PerClientIdStruct) }),
        RdPerContextMap<String, RdSignal<PerClientIdSignal>>(PerClientIdRoot.Key, { RdSignal(PerClientIdSignal) })
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("InnerClass (")
        printer.indent {
            print("someValue = "); _someValue.print(printer); println()
            print("someClassValue = "); _someClassValue.print(printer); println()
            print("someClassSignal = "); _someClassSignal.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): InnerClass   {
        return InnerClass(
            _someValue.deepClonePolymorphic(),
            _someClassValue.deepClonePolymorphic(),
            _someClassSignal.deepClonePolymorphic()
        )
    }
    //contexts
}


/**
 * #### Generated from [PerClientId.kt:24]
 */
class PerClientIdSignal (
) : IPrintable {
    //companion
    
    companion object : IMarshaller<PerClientIdSignal> {
        override val _type: KClass<PerClientIdSignal> = PerClientIdSignal::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): PerClientIdSignal  {
            return PerClientIdSignal()
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: PerClientIdSignal)  {
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
        
        other as PerClientIdSignal
        
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("PerClientIdSignal (")
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [PerClientId.kt:23]
 */
class PerClientIdStruct (
) : IPrintable {
    //companion
    
    companion object : IMarshaller<PerClientIdStruct> {
        override val _type: KClass<PerClientIdStruct> = PerClientIdStruct::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): PerClientIdStruct  {
            return PerClientIdStruct()
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: PerClientIdStruct)  {
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
        
        other as PerClientIdStruct
        
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("PerClientIdStruct (")
        printer.print(")")
    }
    //deepClone
    //contexts
}
