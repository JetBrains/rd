@file:Suppress("PackageDirectoryMismatch", "UnusedImport", "unused", "LocalVariableName")
package org.example

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.framework.impl.*

import com.jetbrains.rd.util.lifetime.*
import com.jetbrains.rd.util.reactive.*
import com.jetbrains.rd.util.string.*
import com.jetbrains.rd.util.*
import kotlin.reflect.KClass



class DemoModel private constructor(
    private val _boolean_property: RdOptionalProperty<Boolean>,
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
        
        
        const val serializationHash = -7851046433962724830L
    }
    override val serializersOwner: ISerializersOwner get() = DemoModel
    override val serializationHash: Long get() = DemoModel.serializationHash
    
    //fields
    val boolean_property: IOptProperty<Boolean> get() = _boolean_property
    val scalar: IOptProperty<MyScalar> get() = _scalar
    val list: IMutableViewableList<Int> get() = _list
    val set: IMutableViewableSet<Int> get() = _set
    val mapLongToString: IMutableViewableMap<Long, String> get() = _mapLongToString
    val call: RdEndpoint<Char, String> get() = _call
    val callback: IRdCall<String, Int> get() = _callback
    //initializer
    init {
        _boolean_property.optimizeNested = true
        _scalar.optimizeNested = true
        _list.optimizeNested = true
        _set.optimizeNested = true
        _mapLongToString.optimizeNested = true
    }
    
    init {
        _mapLongToString.master = false
    }
    
    init {
        bindableChildren.add("boolean_property" to _boolean_property)
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
            print("boolean_property = "); _boolean_property.print(printer); println()
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
    val sign: Boolean,
    val byte: Byte,
    val short: Short,
    val int: Int,
    val long: Long
) : IPrintable {
    //companion
    
    companion object : IMarshaller<MyScalar> {
        override val _type: KClass<MyScalar> = MyScalar::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): MyScalar {
            val sign = buffer.readBool()
            val byte = buffer.readByte()
            val short = buffer.readShort()
            val int = buffer.readInt()
            val long = buffer.readLong()
            return MyScalar(sign, byte, short, int, long)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: MyScalar) {
            buffer.writeBool(value.sign)
            buffer.writeByte(value.byte)
            buffer.writeShort(value.short)
            buffer.writeInt(value.int)
            buffer.writeLong(value.long)
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
        
        if (sign != other.sign) return false
        if (byte != other.byte) return false
        if (short != other.short) return false
        if (int != other.int) return false
        if (long != other.long) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int {
        var __r = 0
        __r = __r*31 + sign.hashCode()
        __r = __r*31 + byte.hashCode()
        __r = __r*31 + short.hashCode()
        __r = __r*31 + int.hashCode()
        __r = __r*31 + long.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter) {
        printer.println("MyScalar (")
        printer.indent {
            print("sign = "); sign.print(printer); println()
            print("byte = "); byte.print(printer); println()
            print("short = "); short.print(printer); println()
            print("int = "); int.print(printer); println()
            print("long = "); long.print(printer); println()
        }
        printer.print(")")
    }
}
