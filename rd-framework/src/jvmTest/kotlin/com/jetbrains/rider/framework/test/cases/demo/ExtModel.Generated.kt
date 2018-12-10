@file:Suppress("PackageDirectoryMismatch", "UnusedImport", "unused", "LocalVariableName")
package com.jetbrains.rider.framework.test.cases.demo

import com.jetbrains.rider.framework.*
import com.jetbrains.rider.framework.base.*
import com.jetbrains.rider.framework.impl.*

import com.jetbrains.rider.util.reactive.*
import com.jetbrains.rider.util.string.*


class ExtModel private constructor(
    private val _checker: RdSignal<Unit>
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers) {
        }
        
        
        
    }
    override val serializersOwner: ISerializersOwner get() = ExtModel
    override val serializationHash: Long get() = 2364843396187734L
    
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

