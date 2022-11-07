@file:Suppress("EXPERIMENTAL_API_USAGE","EXPERIMENTAL_UNSIGNED_LITERALS","PackageDirectoryMismatch","UnusedImport","unused","LocalVariableName","CanBeVal","PropertyName","EnumEntryName","ClassName","ObjectPropertyName","UnnecessaryVariable","SpellCheckingInspection")
package call.generated.code.root

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
 * #### Generated from [CallGeneratedCodeTest.kt:17]
 */
class CallGeneratedCodeRoot private constructor(
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            serializers.register(Editor)
            serializers.register(Es.marshaller)
            serializers.register(Abc)
            CallGeneratedCodeRoot.register(serializers)
        }
        
        
        
        
        
        const val serializationHash = 2426278172729483988L
        
    }
    override val serializersOwner: ISerializersOwner get() = CallGeneratedCodeRoot
    override val serializationHash: Long get() = CallGeneratedCodeRoot.serializationHash
    
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("CallGeneratedCodeRoot (")
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): CallGeneratedCodeRoot   {
        return CallGeneratedCodeRoot(
        )
    }
    //contexts
}


/**
 * #### Generated from [CallGeneratedCodeTest.kt:29]
 */
class Abc (
) : IPrintable {
    //companion
    
    companion object : IMarshaller<Abc> {
        override val _type: KClass<Abc> = Abc::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): Abc  {
            return Abc()
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: Abc)  {
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
        
        other as Abc
        
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("Abc (")
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [CallGeneratedCodeTest.kt:18]
 */
class Editor private constructor(
    val documentName: Int,
    private val _caret: RdOptionalProperty<Int>,
    val singleLine: Boolean = false,
    val es: EnumSet<Es> = enumSetOf(),
    val xxx: List<Abc>? = null
) : RdBindableBase() {
    //companion
    
    companion object : IMarshaller<Editor> {
        override val _type: KClass<Editor> = Editor::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): Editor  {
            val _id = RdId.read(buffer)
            val documentName = buffer.readInt()
            val _caret = RdOptionalProperty.read(ctx, buffer, FrameworkMarshallers.Int)
            val singleLine = buffer.readBool()
            val es = buffer.readEnumSet<Es>()
            val xxx = buffer.readNullable { buffer.readList { ctx.readInterned(buffer, "Protocol") { _, _ -> Abc.read(ctx, buffer) } } }
            return Editor(documentName, _caret, singleLine, es, xxx).withId(_id)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: Editor)  {
            value.rdid.write(buffer)
            buffer.writeInt(value.documentName)
            RdOptionalProperty.write(ctx, buffer, value._caret)
            buffer.writeBool(value.singleLine)
            buffer.writeEnumSet(value.es)
            buffer.writeNullable(value.xxx) { buffer.writeList(it) { v -> ctx.writeInterned(buffer, v, "Protocol") { _, _, internedValue -> Abc.write(ctx, buffer, internedValue) } } }
        }
        
        
    }
    //fields
    val caret: IOptProperty<Int> get() = _caret
    //methods
    //initializer
    init {
        _caret.optimizeNested = true
    }
    
    init {
        bindableChildren.add("caret" to _caret)
    }
    
    //secondary constructor
    constructor(
        documentName: Int,
        singleLine: Boolean = false,
        es: EnumSet<Es> = enumSetOf(),
        xxx: List<Abc>? = null
    ) : this(
        documentName,
        RdOptionalProperty<Int>(FrameworkMarshallers.Int),
        singleLine,
        es,
        xxx
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("Editor (")
        printer.indent {
            print("documentName = "); documentName.print(printer); println()
            print("caret = "); _caret.print(printer); println()
            print("singleLine = "); singleLine.print(printer); println()
            print("es = "); es.print(printer); println()
            print("xxx = "); xxx.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): Editor   {
        return Editor(
            documentName,
            _caret.deepClonePolymorphic(),
            singleLine,
            es,
            xxx
        )
    }
    //contexts
}


/**
 * #### Generated from [CallGeneratedCodeTest.kt:23]
 */
enum class Es {
    a, 
    b, 
    c;
    
    companion object {
        val marshaller = FrameworkMarshallers.enumSet<Es>()
        
    }
}
