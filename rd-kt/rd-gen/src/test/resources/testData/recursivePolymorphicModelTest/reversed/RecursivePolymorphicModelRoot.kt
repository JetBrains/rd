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
 * #### Generated from [RecursivePolymorphicModel.kt:9]
 */
class RecursivePolymorphicModelRoot private constructor(
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            RecursivePolymorphicModelRoot.register(serializers)
            RecursivePolymorphicModel.register(serializers)
        }
        
        
        
        
        
        const val serializationHash = -6059810059586225403L
        
    }
    override val serializersOwner: ISerializersOwner get() = RecursivePolymorphicModelRoot
    override val serializationHash: Long get() = RecursivePolymorphicModelRoot.serializationHash
    
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("RecursivePolymorphicModelRoot (")
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): RecursivePolymorphicModelRoot   {
        return RecursivePolymorphicModelRoot(
        )
    }
    //contexts
    //threading
    override val extThreading: ExtThreadingKind get() = ExtThreadingKind.Default
}
