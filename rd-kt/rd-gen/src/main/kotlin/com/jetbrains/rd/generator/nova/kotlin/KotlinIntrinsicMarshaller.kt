package com.jetbrains.rd.generator.nova.kotlin


/**
 * [marshallerObjectFqn] Fqn of object: inheritor of IMarshaller
 */
data class KotlinIntrinsicMarshaller(val marshallerObjectFqn:String?, val rdid: Long? = null) {
    companion object {
        val default = KotlinIntrinsicMarshaller(null)
    }
}