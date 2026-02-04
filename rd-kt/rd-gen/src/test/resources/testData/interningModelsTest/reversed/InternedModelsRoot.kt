@file:Suppress("EXPERIMENTAL_API_USAGE","EXPERIMENTAL_UNSIGNED_LITERALS","PackageDirectoryMismatch","UnusedImport","unused","LocalVariableName","CanBeVal","PropertyName","EnumEntryName","ClassName","ObjectPropertyName","UnnecessaryVariable","SpellCheckingInspection")
package InternedModelsRoot

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
 * #### Generated from [InterningModelsTest.kt:14]
 */
class InternedModelsRoot private constructor(
    private val _editors: RdMap<Int, Editor>
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            val classLoader = javaClass.classLoader
            serializers.register(LazyCompanionMarshaller(RdId(18933576544), classLoader, "InternedModelsRoot.Editor"))
            serializers.register(LazyCompanionMarshaller(RdId(631631), classLoader, "InternedModelsRoot.Abc"))
            InternedModelsRoot.register(serializers)
        }
        
        
        
        
        
        const val serializationHash = 207990610366166095L
        
    }
    override val serializersOwner: ISerializersOwner get() = InternedModelsRoot
    override val serializationHash: Long get() = InternedModelsRoot.serializationHash
    
    //fields
    val editors: IMutableViewableMap<Int, Editor> get() = _editors
    //methods
    //initializer
    init {
        bindableChildren.add("editors" to _editors)
    }
    
    //secondary constructor
    private constructor(
    ) : this(
        RdMap<Int, Editor>(FrameworkMarshallers.Int, Editor)
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("InternedModelsRoot (")
        printer.indent {
            print("editors = "); _editors.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): InternedModelsRoot   {
        return InternedModelsRoot(
            _editors.deepClonePolymorphic()
        )
    }
    //contexts
    //threading
    override val extThreading: ExtThreadingKind get() = ExtThreadingKind.Default
}


/**
 * #### Generated from [InterningModelsTest.kt:16]
 */
class Abc (
) : IPrintable {
    //write-marshaller
    private fun write(ctx: SerializationCtx, buffer: AbstractBuffer)  {
    }
    //companion
    
    companion object : IMarshaller<Abc> {
        override val _type: KClass<Abc> = Abc::class
        override val id: RdId get() = RdId(631631)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): Abc  {
            return Abc()
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: Abc)  {
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
    //threading
}


/**
 * #### Generated from [InterningModelsTest.kt:15]
 */
class Editor (
    val xxx: List<Abc>? = null
) : RdBindableBase() {
    //write-marshaller
    private fun write(ctx: SerializationCtx, buffer: AbstractBuffer)  {
        rdid.write(buffer)
        buffer.writeNullable(xxx) { buffer.writeList(it) { v -> ctx.writeInterned(buffer, v, "Protocol") { _, _, internedValue -> Abc.write(ctx, buffer, internedValue) } } }
    }
    //companion
    
    companion object : IMarshaller<Editor> {
        override val _type: KClass<Editor> = Editor::class
        override val id: RdId get() = RdId(18933576544)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): Editor  {
            val _id = RdId.read(buffer)
            val xxx = buffer.readNullable { buffer.readList { ctx.readInterned(buffer, "Protocol") { _, _ -> Abc.read(ctx, buffer) } } }
            return Editor(xxx).withId(_id)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: Editor)  {
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
        printer.println("Editor (")
        printer.indent {
            print("xxx = "); xxx.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): Editor   {
        return Editor(
            xxx
        )
    }
    //contexts
    //threading
}
