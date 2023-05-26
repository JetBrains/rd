@file:Suppress("EXPERIMENTAL_API_USAGE","EXPERIMENTAL_UNSIGNED_LITERALS","PackageDirectoryMismatch","UnusedImport","unused","LocalVariableName","CanBeVal","PropertyName","EnumEntryName","ClassName","ObjectPropertyName","UnnecessaryVariable","SpellCheckingInspection")
package CustomThreadingRoot

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
 * #### Generated from [CustomThreadingTest.kt:15]
 */
class CustomThreadingRoot private constructor(
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            CustomThreadingRoot.register(serializers)
        }
        
        
        
        
        
        const val serializationHash = 7790476836924476007L
        
    }
    override val serializersOwner: ISerializersOwner get() = CustomThreadingRoot
    override val serializationHash: Long get() = CustomThreadingRoot.serializationHash
    
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("CustomThreadingRoot (")
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): CustomThreadingRoot   {
        return CustomThreadingRoot(
        )
    }
    //contexts
    //threading
    override val extThreading: ExtThreadingKind get() = ExtThreadingKind.CustomScheduler
    fun setScheduler(scheduler: IScheduler) = setCustomScheduler(scheduler)
}
