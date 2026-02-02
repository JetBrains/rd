@file:Suppress("EXPERIMENTAL_API_USAGE","EXPERIMENTAL_UNSIGNED_LITERALS","PackageDirectoryMismatch","UnusedImport","unused","LocalVariableName","CanBeVal","PropertyName","EnumEntryName","ClassName","ObjectPropertyName","UnnecessaryVariable","SpellCheckingInspection")
package DefaultNlsValuesRoot

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
 * #### Generated from [DefaultNlsValuesTest.kt:14]
 */
class DefaultNlsValuesRoot private constructor(
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            val classLoader = javaClass.classLoader
            serializers.register(LazyCompanionMarshaller(RdId(17439278522805508), classLoader, "DefaultNlsValuesRoot.ClassModel"))
            DefaultNlsValuesRoot.register(serializers)
        }
        
        
        
        
        
        const val serializationHash = -5102364007078219213L
        
    }
    override val serializersOwner: ISerializersOwner get() = DefaultNlsValuesRoot
    override val serializationHash: Long get() = DefaultNlsValuesRoot.serializationHash
    
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("DefaultNlsValuesRoot (")
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): DefaultNlsValuesRoot   {
        return DefaultNlsValuesRoot(
        )
    }
    //contexts
    //threading
    override val extThreading: ExtThreadingKind get() = ExtThreadingKind.Default
}


/**
 * #### Generated from [DefaultNlsValuesTest.kt:15]
 */
class ClassModel private constructor(
    val fff: @org.jetbrains.annotations.Nls String = "123",
    private val _ppp: RdProperty<@org.jetbrains.annotations.Nls String>
) : RdBindableBase() {
    //write-marshaller
    private fun write(ctx: SerializationCtx, buffer: AbstractBuffer)  {
        rdid.write(buffer)
        buffer.writeString(fff)
        RdProperty.write(ctx, buffer, _ppp)
    }
    //companion
    
    companion object : IMarshaller<ClassModel> {
        override val _type: KClass<ClassModel> = ClassModel::class
        override val id: RdId get() = RdId(17439278522805508)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): ClassModel  {
            val _id = RdId.read(buffer)
            val fff = buffer.readString()
            val _ppp = RdProperty.read(ctx, buffer, __StringSerializer)
            return ClassModel(fff, _ppp).withId(_id)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: ClassModel)  {
            value.write(ctx, buffer)
        }
        
        private val __StringSerializer = FrameworkMarshallers.String
        
    }
    //fields
    val ppp: IProperty<@org.jetbrains.annotations.Nls String> get() = _ppp
    //methods
    //initializer
    init {
        _ppp.optimizeNested = true
    }
    
    init {
        bindableChildren.add("ppp" to _ppp)
    }
    
    //secondary constructor
    constructor(
        fff: @org.jetbrains.annotations.Nls String = "123"
    ) : this(
        fff,
        RdProperty<@org.jetbrains.annotations.Nls String>("123", __StringSerializer)
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("ClassModel (")
        printer.indent {
            print("fff = "); fff.print(printer); println()
            print("ppp = "); _ppp.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): ClassModel   {
        return ClassModel(
            fff,
            _ppp.deepClonePolymorphic()
        )
    }
    //contexts
    //threading
}
