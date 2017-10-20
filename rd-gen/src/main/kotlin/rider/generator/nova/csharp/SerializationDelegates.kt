package com.jetbrains.rider.generator.nova.csharp


/**
 *
 * @param read Fully qualified name of ReadDelegate or null if it's default: <typename>.Read
 * @param write Fully qualified name of WriteDelegate or null if it's default: <typename>.Write
 */
data class SerializationDelegates(val read:String?, val write:String?) {
    companion object {
        val default = SerializationDelegates(null, null)
    }
}