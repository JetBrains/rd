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
        
        
        private fun createModel(lifetime: Lifetime, protocol: IProtocol): ExampleRootNova  {
            ExampleRootNova.register(protocol.serializers)
            
            return ExampleRootNova().apply {
                identify(protocol.identity, RdId.Null.mix("ExampleRootNova"))
                bind(lifetime, protocol, "ExampleRootNova")
            }
        }
        
        @JvmStatic
        fun getOrCreate(protocol: IProtocol): ExampleRootNova  {
            return protocol.getOrCreateExtension(ExampleRootNova::class) { createModel(protocol.lifetime, protocol) }
        }
        
        @JvmStatic
        fun getOrNull(protocol: IProtocol): ExampleRootNova?  {
            return protocol.tryGetExtension(ExampleRootNova::class)
        }
        
        @JvmStatic
        fun createOrThrow(protocol: IProtocol): ExampleRootNova  {
            return protocol.createExtensionOrThrow(ExampleRootNova::class) { createModel(protocol.lifetime, protocol) }
        }
        
        @JvmStatic
        @Deprecated("Use getOrCreate(protocol), createOrThrow(protocol) or getOrNull(protocol)", ReplaceWith("ExampleRootNova.createOrThrow(protocol)"))
        fun create(lifetime: Lifetime, protocol: IProtocol): ExampleRootNova  {
            return createOrThrow(protocol)
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
