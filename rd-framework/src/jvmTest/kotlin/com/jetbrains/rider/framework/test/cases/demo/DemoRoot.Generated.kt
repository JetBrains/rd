@file:Suppress("PackageDirectoryMismatch", "UnusedImport", "unused", "LocalVariableName")
package org.example

import com.jetbrains.rider.framework.*
import com.jetbrains.rider.framework.base.*
import com.jetbrains.rider.framework.impl.*

import com.jetbrains.rider.util.lifetime.*
import com.jetbrains.rider.util.reactive.*
import com.jetbrains.rider.util.string.*
import com.jetbrains.rider.util.trace
import com.jetbrains.rider.util.Date
import com.jetbrains.rider.util.UUID
import com.jetbrains.rider.util.URI
import kotlin.reflect.KClass



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
        
        
        const val serializationHash = 2990580803186469991L
    }
    override val serializersOwner: ISerializersOwner get() = DemoRoot
    override val serializationHash: Long get() = DemoRoot.serializationHash
    
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
