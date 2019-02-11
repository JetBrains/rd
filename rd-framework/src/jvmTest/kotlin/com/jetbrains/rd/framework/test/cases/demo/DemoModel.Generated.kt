@file:Suppress("PackageDirectoryMismatch", "UnusedImport", "unused", "LocalVariableName")
package org.example

import com.jetbrains.rider.framework.*
import com.jetbrains.rider.framework.base.*
import com.jetbrains.rider.framework.impl.*

import com.jetbrains.rider.util.lifetime.*
import com.jetbrains.rider.util.reactive.*
import com.jetbrains.rider.util.string.*
import com.jetbrains.rider.util.trace
import com.jetbrains.rider.util.Date
import com.jetbrains.rider.util.UUID
import com.jetbrains.rider.util.URI
import kotlin.reflect.KClass



class DemoModel private constructor(
    private val _bool: RdOptionalProperty<Boolean>,
    private val _scalar: RdOptionalProperty<MyScalar>,
    private val _list: RdList<Int>,
    private val _set: RdSet<Int>,
    private val _mapLongToString: RdMap<Long, String>,
    private val _call: RdEndpoint<Char, String>,
    private val _callback: RdCall<String, Int>
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers) {
            serializers.register(MyScalar)
        }
        
        
        fun create(lifetime: Lifetime, protocol: IProtocol): DemoModel {
            DemoRoot.register(protocol.serializers)
            
            return DemoModel().apply {
                identify(protocol.identity, RdId.Null.mix("DemoModel"))
                bind(lifetime, protocol, "DemoModel")
            }
        }
        
        
        const val serializationHash = 8983867708965535233L
    }
    override val serializersOwner: ISerializersOwner get() = DemoModel
    override val serializationHash: Long get() = DemoModel.serializationHash
    
    //fields
    val bool: IOptProperty<Boolean> get() = _bool
    val scalar: IOptProperty<MyScalar> get() = _scalar
    val list: IMutableViewableList<Int> get() = _list
    val set: IMutableViewableSet<Int> get() = _set
    val mapLongToString: IMutableViewableMap<Long, String> get() = _mapLongToString
    val call: RdEndpoint<Char, String> get() = _call
    val callback: IRdCall<String, Int> get() = _callback
    //initializer
    init {
        _bool.optimizeNested = true
        _scalar.optimizeNested = true
        _list.optimizeNested = true
        _set.optimizeNested = true
        _mapLongToString.optimizeNested = true
    }
    
    init {
        _mapLongToString.master = false
    }
    
    init {
        bindableChildren.add("bool" to _bool)
        bindableChildren.add("scalar" to _scalar)
        bindableChildren.add("list" to _list)
        bindableChildren.add("set" to _set)
        bindableChildren.add("mapLongToString" to _mapLongToString)
        bindableChildren.add("call" to _call)
        bindableChildren.add("callback" to _callback)
    }
    
    //secondary constructor
    private constructor(
    ) : this(
        RdOptionalProperty<Boolean>(FrameworkMarshallers.Bool),
        RdOptionalProperty<MyScalar>(MyScalar),
        RdList<Int>(FrameworkMarshallers.Int),
        RdSet<Int>(FrameworkMarshallers.Int),
        RdMap<Long, String>(FrameworkMarshallers.Long, FrameworkMarshallers.String),
        RdEndpoint<Char, String>(FrameworkMarshallers.Char, FrameworkMarshallers.String),
        RdCall<String, Int>(FrameworkMarshallers.String, FrameworkMarshallers.Int)
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter) {
        printer.println("DemoModel (")
        printer.indent {
            print("bool = "); _bool.print(printer); println()
            print("scalar = "); _scalar.print(printer); println()
            print("list = "); _list.print(printer); println()
            print("set = "); _set.print(printer); println()
            print("mapLongToString = "); _mapLongToString.print(printer); println()
            print("call = "); _call.print(printer); println()
            print("callback = "); _callback.print(printer); println()
        }
        printer.print(")")
    }
}


data class MyScalar (
    val sign_: Boolean,
    val byte_: Byte,
    val short_: Short,
    val int_: Int,
    val long_: Long
) : IPrintable {
    //companion
    
    companion object : IMarshaller<MyScalar> {
        override val _type: KClass<MyScalar> = MyScalar::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): MyScalar {
            val sign_ = buffer.readBool()
            val byte_ = buffer.readByte()
            val short_ = buffer.readShort()
            val int_ = buffer.readInt()
            val long_ = buffer.readLong()
            return MyScalar(sign_, byte_, short_, int_, long_)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: MyScalar) {
            buffer.writeBool(value.sign_)
            buffer.writeByte(value.byte_)
            buffer.writeShort(value.short_)
            buffer.writeInt(value.int_)
            buffer.writeLong(value.long_)
        }
        
    }
    //fields
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as MyScalar
        
        if (sign_ != other.sign_) return false
        if (byte_ != other.byte_) return false
        if (short_ != other.short_) return false
        if (int_ != other.int_) return false
        if (long_ != other.long_) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int {
        var __r = 0
        __r = __r*31 + sign_.hashCode()
        __r = __r*31 + byte_.hashCode()
        __r = __r*31 + short_.hashCode()
        __r = __r*31 + int_.hashCode()
        __r = __r*31 + long_.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter) {
        printer.println("MyScalar (")
        printer.indent {
            print("sign_ = "); sign_.print(printer); println()
            print("byte_ = "); byte_.print(printer); println()
            print("short_ = "); short_.print(printer); println()
            print("int_ = "); int_.print(printer); println()
            print("long_ = "); long_.print(printer); println()
        }
        printer.print(")")
    }
}
