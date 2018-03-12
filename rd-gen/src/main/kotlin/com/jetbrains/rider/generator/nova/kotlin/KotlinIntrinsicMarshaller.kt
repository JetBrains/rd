package com.jetbrains.rider.generator.nova.kotlin


/**
 * [marshallerObjectFqn] Fqn of object: inheritor of IMarshaller
 */
data class KotlinIntrinsicMarshaller(val marshallerObjectFqn:String?) {
    companion object {
        val default = KotlinIntrinsicMarshaller(null)
    }
}