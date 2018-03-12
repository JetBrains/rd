package com.jetbrains.rider.generator.nova.csharp


/**
 *
 * @param readDelegateFqn Fully qualified name of ReadDelegate or null if it's default: <typename>.Read
 * @param writeDelegateFqn Fully qualified name of WriteDelegate or null if it's default: <typename>.Write
 */
data class CSharpIntrinsicMarshaller(val readDelegateFqn:String?, val writeDelegateFqn:String?) {
    companion object {
        val default = CSharpIntrinsicMarshaller(null, null)
    }
}