package com.jetbrains.rd.framework

import com.jetbrains.rd.util.threadLocalWithInitial

/**
 * Describes an [RdContext] which uses [ThreadLocal] as internal storage for values
 */
abstract class ThreadLocalRdContext<T : Any>(key: String, heavy: Boolean, serializer: IMarshaller<T>) : RdContext<T>(key, heavy, serializer) {
    private val internalValue = threadLocalWithInitial<T?> { null }

    /**
     * The current value for this context implemented as thread-local storage
     */
    override var value: T?
        get() = internalValue.get()
        set(value) = internalValue.set(value)
}