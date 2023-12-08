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
 * #### Generated from [AsyncPrimitives.kt:13]
 */
class AsyncPrimitivesExt private constructor(
    private val _asyncProperty: AsyncRdProperty<String>,
    private val _asyncPropertyNullable: AsyncRdProperty<String?>,
    private val _asyncMap: AsyncRdMap<Int, String>,
    private val _asyncSet: AsyncRdSet<Int>
) : DefaultExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
        }
        
        
        @JvmStatic
        @JvmName("internalCreateModel")
        @Deprecated("Use create instead", ReplaceWith("create(lifetime, protocol)"))
        internal fun createModel(lifetime: Lifetime, protocol: IProtocol): AsyncPrimitivesExt  {
            @Suppress("DEPRECATION")
            return create(lifetime, protocol)
        }
        
        @JvmStatic
        @Deprecated("Use protocol.asyncPrimitivesExt or revise the extension scope instead", ReplaceWith("protocol.asyncPrimitivesExt"))
        fun create(lifetime: Lifetime, protocol: IProtocol): AsyncPrimitivesExt  {
            AsyncPrimitivesRoot.register(protocol.serializers)
            
            return AsyncPrimitivesExt()
        }
        
        private val __StringNullableSerializer = FrameworkMarshallers.String.nullable()
        
        const val serializationHash = -5414287245037713638L
        
    }
    override val serializersOwner: ISerializersOwner get() = AsyncPrimitivesExt
    override val serializationHash: Long get() = AsyncPrimitivesExt.serializationHash
    
    //fields
    val asyncProperty: IMutableAsyncProperty<String> get() = _asyncProperty
    val asyncPropertyNullable: IMutableAsyncProperty<String?> get() = _asyncPropertyNullable
    val asyncMap: AsyncRdMap<Int, String> get() = _asyncMap
    val asyncSet: AsyncRdSet<Int> get() = _asyncSet
    //methods
    //initializer
    init {
        _asyncProperty.optimizeNested = true
        _asyncPropertyNullable.optimizeNested = true
        _asyncMap.optimizeNested = true
        _asyncSet.optimizeNested = true
    }
    
    init {
        bindableChildren.add("asyncProperty" to _asyncProperty)
        bindableChildren.add("asyncPropertyNullable" to _asyncPropertyNullable)
        bindableChildren.add("asyncMap" to _asyncMap)
        bindableChildren.add("asyncSet" to _asyncSet)
    }
    
    //secondary constructor
    private constructor(
    ) : this(
        AsyncRdProperty<String>(FrameworkMarshallers.String),
        AsyncRdProperty<String?>(null, __StringNullableSerializer),
        AsyncRdMap<Int, String>(FrameworkMarshallers.Int, FrameworkMarshallers.String),
        AsyncRdSet<Int>(FrameworkMarshallers.Int)
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("AsyncPrimitivesExt (")
        printer.indent {
            print("asyncProperty = "); _asyncProperty.print(printer); println()
            print("asyncPropertyNullable = "); _asyncPropertyNullable.print(printer); println()
            print("asyncMap = "); _asyncMap.print(printer); println()
            print("asyncSet = "); _asyncSet.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): AsyncPrimitivesExt   {
        return AsyncPrimitivesExt(
            _asyncProperty.deepClonePolymorphic(),
            _asyncPropertyNullable.deepClonePolymorphic(),
            _asyncMap.deepClonePolymorphic(),
            _asyncSet.deepClonePolymorphic()
        )
    }
    //contexts
    //threading
    override val extThreading: ExtThreadingKind get() = ExtThreadingKind.Default
}
val IProtocol.asyncPrimitivesExt get() = getOrCreateExtension(AsyncPrimitivesExt::class) { @Suppress("DEPRECATION") AsyncPrimitivesExt.create(lifetime, this) }

