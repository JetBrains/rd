@file:Suppress("EXPERIMENTAL_API_USAGE","PackageDirectoryMismatch","UnusedImport","unused","LocalVariableName","CanBeVal","PropertyName","EnumEntryName","ClassName","ObjectPropertyName")
package demo

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.framework.impl.*

import com.jetbrains.rd.util.lifetime.*
import com.jetbrains.rd.util.reactive.*
import com.jetbrains.rd.util.string.*
import com.jetbrains.rd.util.*
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
