@file:Suppress("EXPERIMENTAL_API_USAGE","EXPERIMENTAL_UNSIGNED_LITERALS","PackageDirectoryMismatch","UnusedImport","unused","LocalVariableName","CanBeVal","PropertyName","EnumEntryName","ClassName","ObjectPropertyName","UnnecessaryVariable","SpellCheckingInspection")
package ExtensionRoot

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
 * #### Generated from [Extension.kt:11]
 */
class ExtensionRoot private constructor(
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            val classLoader = javaClass.classLoader
            serializers.register(LazyCompanionMarshaller(RdId(-1687597404575351130), classLoader, "ExtensionRoot.ClassWithStr"))
            serializers.register(LazyCompanionMarshaller(RdId(-2602185343174852445), classLoader, "ExtensionRoot.StructWithStr"))
            ExtensionRoot.register(serializers)
        }
        
        
        
        
        
        const val serializationHash = -8799809714061118005L
        
    }
    override val serializersOwner: ISerializersOwner get() = ExtensionRoot
    override val serializationHash: Long get() = ExtensionRoot.serializationHash
    
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("ExtensionRoot (")
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): ExtensionRoot   {
        return ExtensionRoot(
        )
    }
    //contexts
    //threading
    override val extThreading: ExtThreadingKind get() = ExtThreadingKind.Default
}


/**
 * #### Generated from [Extension.kt:23]
 */
class ClassWithStr (
    val extFromClass1: @org.jetbrains.annotations.Nls String?,
    private val _extFromClass2: String,
    val extFromClass3: String?,
    val reallyStrFromClass: @org.jetbrains.annotations.Nls String
) : RdBindableBase() {
    //write-marshaller
    private fun write(ctx: SerializationCtx, buffer: AbstractBuffer)  {
        rdid.write(buffer)
        buffer.writeNullable(extFromClass1) { buffer.writeString(it) }
        buffer.writeNullable(_extFromClass2) { buffer.writeString(it) }
        buffer.writeNullable(extFromClass3) { buffer.writeString(it) }
        buffer.writeString(reallyStrFromClass)
    }
    //companion
    
    companion object : IMarshaller<ClassWithStr> {
        override val _type: KClass<ClassWithStr> = ClassWithStr::class
        override val id: RdId get() = RdId(-1687597404575351130)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): ClassWithStr  {
            val _id = RdId.read(buffer)
            val extFromClass1 = buffer.readNullable { buffer.readString() }
            val _extFromClass2 = com.jetbrains.rd.generator.test.cases.generator.testModels.fqn(buffer.readNullable { buffer.readString() })
            val extFromClass3 = buffer.readNullable { buffer.readString() }
            val reallyStrFromClass = buffer.readString()
            return ClassWithStr(extFromClass1, _extFromClass2, extFromClass3, reallyStrFromClass).withId(_id)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: ClassWithStr)  {
            value.write(ctx, buffer)
        }
        
        
    }
    //fields
    val extFromClass2: String get() = _extFromClass2
    //methods
    //initializer
    //secondary constructor
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("ClassWithStr (")
        printer.indent {
            print("extFromClass1 = "); extFromClass1.print(printer); println()
            print("extFromClass2 = "); _extFromClass2.print(printer); println()
            print("extFromClass3 = "); extFromClass3.print(printer); println()
            print("reallyStrFromClass = "); reallyStrFromClass.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): ClassWithStr   {
        return ClassWithStr(
            extFromClass1,
            _extFromClass2,
            extFromClass3,
            reallyStrFromClass
        )
    }
    //contexts
    //threading
}


/**
 * #### Generated from [Extension.kt:29]
 */
data class StructWithStr (
    val extFromStruct1: @org.jetbrains.annotations.Nls String?,
    private val _extFromStruct2: String,
    val extFromStruct3: String?,
    val reallyStrFromStruct: @org.jetbrains.annotations.Nls String
) : IPrintable {
    //write-marshaller
    private fun write(ctx: SerializationCtx, buffer: AbstractBuffer)  {
        buffer.writeNullable(extFromStruct1) { buffer.writeString(it) }
        buffer.writeNullable(_extFromStruct2) { buffer.writeString(it) }
        buffer.writeNullable(extFromStruct3) { buffer.writeString(it) }
        buffer.writeString(reallyStrFromStruct)
    }
    //companion
    
    companion object : IMarshaller<StructWithStr> {
        override val _type: KClass<StructWithStr> = StructWithStr::class
        override val id: RdId get() = RdId(-2602185343174852445)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): StructWithStr  {
            val extFromStruct1 = buffer.readNullable { buffer.readString() }
            val _extFromStruct2 = com.jetbrains.rd.generator.test.cases.generator.testModels.fqn(buffer.readNullable { buffer.readString() })
            val extFromStruct3 = buffer.readNullable { buffer.readString() }
            val reallyStrFromStruct = buffer.readString()
            return StructWithStr(extFromStruct1, _extFromStruct2, extFromStruct3, reallyStrFromStruct)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: StructWithStr)  {
            value.write(ctx, buffer)
        }
        
        
    }
    //fields
    val extFromStruct2: String get() = _extFromStruct2
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as StructWithStr
        
        if (extFromStruct1 != other.extFromStruct1) return false
        if (_extFromStruct2 != other._extFromStruct2) return false
        if (extFromStruct3 != other.extFromStruct3) return false
        if (reallyStrFromStruct != other.reallyStrFromStruct) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + if (extFromStruct1 != null) extFromStruct1.hashCode() else 0
        __r = __r*31 + if (_extFromStruct2 != null) _extFromStruct2.hashCode() else 0
        __r = __r*31 + if (extFromStruct3 != null) extFromStruct3.hashCode() else 0
        __r = __r*31 + reallyStrFromStruct.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("StructWithStr (")
        printer.indent {
            print("extFromStruct1 = "); extFromStruct1.print(printer); println()
            print("extFromStruct2 = "); _extFromStruct2.print(printer); println()
            print("extFromStruct3 = "); extFromStruct3.print(printer); println()
            print("reallyStrFromStruct = "); reallyStrFromStruct.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}
