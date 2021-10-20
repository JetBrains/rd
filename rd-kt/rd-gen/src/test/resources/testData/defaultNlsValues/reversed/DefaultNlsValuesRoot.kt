@file:Suppress("EXPERIMENTAL_API_USAGE","EXPERIMENTAL_UNSIGNED_LITERALS","PackageDirectoryMismatch","UnusedImport","unused","LocalVariableName","CanBeVal","PropertyName","EnumEntryName","ClassName","ObjectPropertyName","UnnecessaryVariable","SpellCheckingInspection")
package DefaultNlsValuesRoot

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
 * #### Generated from [DefaultNlsValuesTest.kt:14]
 */
class DefaultNlsValuesRoot private constructor(
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            serializers.register(ClassModel)
            DefaultNlsValuesRoot.register(serializers)
        }
        
        
        @JvmStatic
        fun create(lifetime: Lifetime, protocol: IProtocol): DefaultNlsValuesRoot  {
            DefaultNlsValuesRoot.register(protocol.serializers)
            
            return DefaultNlsValuesRoot().apply {
                identify(protocol.identity, RdId.Null.mix("DefaultNlsValuesRoot"))
                bind(lifetime, protocol, "DefaultNlsValuesRoot")
            }
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
}


/**
 * #### Generated from [DefaultNlsValuesTest.kt:15]
 */
class ClassModel private constructor(
    val fff: @org.jetbrains.annotations.Nls String = "123",
    private val _ppp: RdProperty<@org.jetbrains.annotations.Nls String>
) : RdBindableBase() {
    //companion
    
    companion object : IMarshaller<ClassModel> {
        override val _type: KClass<ClassModel> = ClassModel::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): ClassModel  {
            val _id = RdId.read(buffer)
            val fff = buffer.readString()
            val _ppp = RdProperty.read(ctx, buffer, __StringSerializer)
            return ClassModel(fff, _ppp).withId(_id)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: ClassModel)  {
            value.rdid.write(buffer)
            buffer.writeString(value.fff)
            RdProperty.write(ctx, buffer, value._ppp)
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
}
