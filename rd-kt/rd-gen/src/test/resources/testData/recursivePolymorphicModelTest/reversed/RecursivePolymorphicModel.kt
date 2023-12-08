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
 * #### Generated from [RecursivePolymorphicModel.kt:14]
 */
class RecursivePolymorphicModel private constructor(
    private val _line: RdOptionalProperty<BeTreeGridLine>,
    private val _list: RdOptionalProperty<List<BeTreeGridLine>>
) : DefaultExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            val classLoader = javaClass.classLoader
            serializers.register(LazyCompanionMarshaller(RdId(8163186073645594862), classLoader, "org.example.BeTreeGridLine"))
            serializers.register(LazyCompanionMarshaller(RdId(7212854712083994073), classLoader, "org.example.BeTreeGridLine_Unknown"))
        }
        
        
        @JvmStatic
        @JvmName("internalCreateModel")
        @Deprecated("Use create instead", ReplaceWith("create(lifetime, protocol)"))
        internal fun createModel(lifetime: Lifetime, protocol: IProtocol): RecursivePolymorphicModel  {
            @Suppress("DEPRECATION")
            return create(lifetime, protocol)
        }
        
        @JvmStatic
        @Deprecated("Use protocol.recursivePolymorphicModel or revise the extension scope instead", ReplaceWith("protocol.recursivePolymorphicModel"))
        fun create(lifetime: Lifetime, protocol: IProtocol): RecursivePolymorphicModel  {
            RecursivePolymorphicModelRoot.register(protocol.serializers)
            
            return RecursivePolymorphicModel()
        }
        
        private val __BeTreeGridLineListSerializer = AbstractPolymorphic(BeTreeGridLine).list()
        
        const val serializationHash = 4259101978417261843L
        
    }
    override val serializersOwner: ISerializersOwner get() = RecursivePolymorphicModel
    override val serializationHash: Long get() = RecursivePolymorphicModel.serializationHash
    
    //fields
    val line: IOptProperty<BeTreeGridLine> get() = _line
    val list: IOptProperty<List<BeTreeGridLine>> get() = _list
    //methods
    //initializer
    init {
        bindableChildren.add("line" to _line)
        bindableChildren.add("list" to _list)
    }
    
    //secondary constructor
    private constructor(
    ) : this(
        RdOptionalProperty<BeTreeGridLine>(AbstractPolymorphic(BeTreeGridLine)),
        RdOptionalProperty<List<BeTreeGridLine>>(__BeTreeGridLineListSerializer)
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("RecursivePolymorphicModel (")
        printer.indent {
            print("line = "); _line.print(printer); println()
            print("list = "); _list.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): RecursivePolymorphicModel   {
        return RecursivePolymorphicModel(
            _line.deepClonePolymorphic(),
            _list.deepClonePolymorphic()
        )
    }
    //contexts
    //threading
    override val extThreading: ExtThreadingKind get() = ExtThreadingKind.Default
}
val IProtocol.recursivePolymorphicModel get() = getOrCreateExtension(RecursivePolymorphicModel::class) { @Suppress("DEPRECATION") RecursivePolymorphicModel.create(lifetime, this) }



/**
 * #### Generated from [RecursivePolymorphicModel.kt:15]
 */
open class BeTreeGridLine protected constructor(
    protected val _children: RdList<BeTreeGridLine>
) : RdBindableBase() {
    //companion
    
    companion object : IMarshaller<BeTreeGridLine>, IAbstractDeclaration<BeTreeGridLine> {
        override val _type: KClass<BeTreeGridLine> = BeTreeGridLine::class
        override val id: RdId get() = RdId(8163186073645594862)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): BeTreeGridLine  {
            val _id = RdId.read(buffer)
            val _children = RdList.read(ctx, buffer, AbstractPolymorphic(BeTreeGridLine))
            return BeTreeGridLine(_children).withId(_id)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: BeTreeGridLine)  {
            value.rdid.write(buffer)
            RdList.write(ctx, buffer, value._children)
        }
        
        
        override fun readUnknownInstance(ctx: SerializationCtx, buffer: AbstractBuffer, unknownId: RdId, size: Int): BeTreeGridLine  {
            val objectStartPosition = buffer.position
            val _id = RdId.read(buffer)
            val _children = RdList.read(ctx, buffer, AbstractPolymorphic(BeTreeGridLine))
            val unknownBytes = ByteArray(objectStartPosition + size - buffer.position)
            buffer.readByteArrayRaw(unknownBytes)
            return BeTreeGridLine_Unknown(_children, unknownId, unknownBytes).withId(_id)
        }
        
    }
    //fields
    val children: IMutableViewableList<BeTreeGridLine> get() = _children
    //methods
    //initializer
    init {
        bindableChildren.add("children" to _children)
    }
    
    //secondary constructor
    constructor(
    ) : this(
        RdList<BeTreeGridLine>(AbstractPolymorphic(BeTreeGridLine))
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("BeTreeGridLine (")
        printer.indent {
            print("children = "); _children.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): BeTreeGridLine   {
        return BeTreeGridLine(
            _children.deepClonePolymorphic()
        )
    }
    //contexts
    //threading
}


class BeTreeGridLine_Unknown (
    _children: RdList<BeTreeGridLine>,
    override val unknownId: RdId,
    val unknownBytes: ByteArray
) : BeTreeGridLine (
    _children
), IUnknownInstance {
    //companion
    
    companion object : IMarshaller<BeTreeGridLine_Unknown> {
        override val _type: KClass<BeTreeGridLine_Unknown> = BeTreeGridLine_Unknown::class
        override val id: RdId get() = RdId(7212854712083994073)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): BeTreeGridLine_Unknown  {
            throw NotImplementedError("Unknown instances should not be read via serializer")
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: BeTreeGridLine_Unknown)  {
            value.rdid.write(buffer)
            RdList.write(ctx, buffer, value._children)
            buffer.writeByteArrayRaw(value.unknownBytes)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    constructor(
        unknownId: RdId,
        unknownBytes: ByteArray
    ) : this(
        RdList<BeTreeGridLine>(AbstractPolymorphic(BeTreeGridLine)),
        unknownId,
        unknownBytes
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("BeTreeGridLine_Unknown (")
        printer.indent {
            print("children = "); _children.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): BeTreeGridLine_Unknown   {
        return BeTreeGridLine_Unknown(
            _children.deepClonePolymorphic(),
            unknownId,
            unknownBytes
        )
    }
    //contexts
    //threading
}
