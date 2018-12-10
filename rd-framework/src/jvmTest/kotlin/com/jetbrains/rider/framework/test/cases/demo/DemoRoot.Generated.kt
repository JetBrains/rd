@file:Suppress("PackageDirectoryMismatch", "UnusedImport", "unused", "LocalVariableName")
package com.jetbrains.rider.framework.test.cases.demo

import com.jetbrains.rider.framework.*
import com.jetbrains.rider.framework.base.*

import com.jetbrains.rider.util.lifetime.*
import com.jetbrains.rider.util.string.*


class DemoRoot private constructor(
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers) {
            DemoRoot.register(serializers)
            DemoModel.register(serializers)
            ExtModel.register(serializers)
        }
        
        
        fun create(lifetime: Lifetime, protocol: IProtocol): DemoRoot {
            DemoRoot.register(protocol.serializers)
            
            return DemoRoot().apply {
                identify(protocol.identity, RdId.Null.mix("DemoRoot"))
                bind(lifetime, protocol, "DemoRoot")
            }
        }
        
    }
    override val serializersOwner: ISerializersOwner get() = DemoRoot
    override val serializationHash: Long get() = 2990580803186469991L
    
    //fields
    //initializer
    //secondary constructor
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter) {
        printer.println("DemoRoot (")
        printer.print(")")
    }
}
