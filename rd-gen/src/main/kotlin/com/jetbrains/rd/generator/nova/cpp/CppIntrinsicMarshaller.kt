package com.jetbrains.rd.generator.nova.cpp


/**
 * [marshallerObjectFqn] Fqn of object: inheritor of IMarshaller
 */
data class CppIntrinsicMarshaller(val marshallerObjectFqn:String?) {
    companion object {
        val default = CppIntrinsicMarshaller(null)
    }
}