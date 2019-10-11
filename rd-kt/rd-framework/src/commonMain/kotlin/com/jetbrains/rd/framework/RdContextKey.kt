package com.jetbrains.rd.framework

import com.jetbrains.rd.util.threadLocalWithInitial

/**
 * Describes a context key. RdContextLocals with matching registered keys will be synchronized via protocol.
 * A heavy key maintains a value set and interns values. A light key sends values as-is and does not maintain a value set.
 */
data class RdContextKey<T : Any>(val key: String, val heavy: Boolean, val lightSerializer: IMarshaller<T>) {
    companion object {
        internal val myValues = threadLocalWithInitial { HashMap<String, Any>() }

        internal fun unsafeSet(key: String, value: Any?) {
            if(value == null)
                myValues.get().remove(key)
            else
                myValues.get()[key] = value
        }

        internal fun unsafeGet(key: String) : Any? {
            return myValues.get()[key]
        }
    }

    @Suppress("UNCHECKED_CAST")
    var value: T?
        get() = unsafeGet(key) as T?
        set(value) = unsafeSet(key, value)
}