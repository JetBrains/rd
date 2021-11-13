@file:Suppress("EXPERIMENTAL_API_USAGE","EXPERIMENTAL_UNSIGNED_LITERALS","PackageDirectoryMismatch","UnusedImport","unused","LocalVariableName","CanBeVal","PropertyName","EnumEntryName","ClassName","ObjectPropertyName","UnnecessaryVariable","SpellCheckingInspection")
package org.example

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
 * #### Generated from [Example.kt:13]
 */
class ExampleRootNova private constructor(
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            ExampleRootNova.register(serializers)
            ExampleModelNova.register(serializers)
        }
        
        
        @JvmStatic
        fun create(lifetime: Lifetime, protocol: IProtocol): ExampleRootNova  {
            ExampleRootNova.register(protocol.serializers)
            
            return ExampleRootNova().apply {
                identify(protocol.identity, RdId.Null.mix("ExampleRootNova"))
                bind(lifetime, protocol, "ExampleRootNova")
            }
        }
        
        
        const val serializationHash = -1365062388667980170L
        
    }
    override val serializersOwner: ISerializersOwner get() = ExampleRootNova
    override val serializationHash: Long get() = ExampleRootNova.serializationHash
    
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("ExampleRootNova (")
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): ExampleRootNova   {
        return ExampleRootNova(
        )
    }
    //contexts
}
