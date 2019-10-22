package com.jetbrains.rd.framework

import com.jetbrains.rd.util.threadLocalWithInitial

/**
 * Describes a context key and provides access to value associated with this key.
 * The associated value is thread-local and synchronized between send/advise pairs on [IWire]. The associated value will be the same in handler method in [IWire.advise] as it was in [IWire.send].
 * Instances of this class with the same [key] will share the associated value.
 * Best practice is to declare context keys in toplevel entities in protocol model using [Toplevel.contextKey]. Manual declaration is also possible.
 * @see com.jetbrains.rd.generator.nova.Toplevel.contextKey
 *
 * @param key textual name of this key. This is used to match this with protocol counterparts
 * @param heavy Whether or not this key is heavy. A heavy key maintains a value set and interns values. A light key sends values as-is and does not maintain a value set.
 * @param serializer Serializer to be used with this key.
 */
data class RdContextKey<T : Any>(val key: String, val heavy: Boolean, val serializer: IMarshaller<T>) {
    companion object {
        private val myValues = threadLocalWithInitial { HashMap<String, Any>() }

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

    /**
     * The current (thread-local) value for this key
     */
    @Suppress("UNCHECKED_CAST")
    var value: T?
        get() = unsafeGet(key) as T?
        set(value) = unsafeSet(key, value)
}