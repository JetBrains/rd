@file:Suppress("PackageDirectoryMismatch", "UnusedImport", "unused", "LocalVariableName")
package org.example

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.framework.impl.*

import com.jetbrains.rd.util.lifetime.*
import com.jetbrains.rd.util.reactive.*
import com.jetbrains.rd.util.string.*
import com.jetbrains.rd.util.*
import kotlin.reflect.KClass



class ExtModel private constructor(
    private val _checker: RdSignal<Unit>
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers) {
        }
        
        
        
        
        const val serializationHash = 2364843396187734L
    }
    override val serializersOwner: ISerializersOwner get() = ExtModel
    override val serializationHash: Long get() = ExtModel.serializationHash
    
    //fields
    val checker: ISignal<Unit> get() = _checker
    //initializer
    init {
        bindableChildren.add("checker" to _checker)
    }
    
    //secondary constructor
    internal constructor(
    ) : this(
        RdSignal<Unit>(FrameworkMarshallers.Void)
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter) {
        printer.println("ExtModel (")
        printer.indent {
            print("checker = "); _checker.print(printer); println()
        }
        printer.print(")")
    }
}
val DemoModel.extModel get() = getOrCreateExtension("extModel", ::ExtModel)

