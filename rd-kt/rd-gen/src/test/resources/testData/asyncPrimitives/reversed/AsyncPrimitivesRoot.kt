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
 * #### Generated from [AsyncPrimitives.kt:8]
 */
class AsyncPrimitivesRoot private constructor(
) : DefaultExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            AsyncPrimitivesRoot.register(serializers)
            AsyncPrimitivesExt.register(serializers)
        }
        
        
        
        
        
        const val serializationHash = -3807429810621465400L
        
    }
    override val serializersOwner: ISerializersOwner get() = AsyncPrimitivesRoot
    override val serializationHash: Long get() = AsyncPrimitivesRoot.serializationHash
    
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("AsyncPrimitivesRoot (")
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): AsyncPrimitivesRoot   {
        return AsyncPrimitivesRoot(
        )
    }
    //contexts
    //threading
    override val extThreading: ExtThreadingKind get() = ExtThreadingKind.Default
}
