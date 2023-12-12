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
 * This is a documentation test,
 * and it is also multiline.
 * #### Generated from [ModelWithDocumentation.kt:11]
 */
class DocumentationModelRoot private constructor(
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            DocumentationModelRoot.register(serializers)
        }
        
        
        
        
        
        const val serializationHash = -7782552752810534509L
        
    }
    override val serializersOwner: ISerializersOwner get() = DocumentationModelRoot
    override val serializationHash: Long get() = DocumentationModelRoot.serializationHash
    
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("DocumentationModelRoot (")
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): DocumentationModelRoot   {
        return DocumentationModelRoot(
        )
    }
    //contexts
    //threading
    override val extThreading: ExtThreadingKind get() = ExtThreadingKind.Default
}
