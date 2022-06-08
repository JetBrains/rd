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
    override val value: T?
        get() = internalValue.get()

    override fun updateValue(newValue: T?): AutoCloseable {
        val newValueSetThread = Thread.currentThread()
        val oldValue = internalValue.get()
        internalValue.set(newValue)
        return AutoCloseable {
            val currentThread = Thread.currentThread()
            assert(newValueSetThread.equals(currentThread)) {
                "Value update cookie must be closed on the same thread (newValue was set on $newValueSetThread, but current thread is $currentThread)"
            }
            internalValue.set(oldValue)
        }
    }
}