package com.jetbrains.rd.framework

import com.jetbrains.rd.util.threadLocalWithInitial

/**
 * Describes a context and provides access to value associated with this context.
 * The associated value is thread-local and synchronized between send/advise pairs on [IWire]. The associated value will be the same in handler method in [IWire.advise] as it was in [IWire.send].
 * Instances of this class with the same [key] will share the associated value.
 * Best practice is to declare contexts in toplevel entities in protocol model using [Toplevel.context]. Manual declaration is also possible.
 * @see com.jetbrains.rd.generator.nova.Toplevel.context
 *
 * @param key textual name of this context. This is used to match this with protocol counterparts
 * @param heavy Whether or not this context is heavy. A heavy context maintains a value set and interns values. A light context sends values as-is and does not maintain a value set.
 * @param serializer Serializer to be used with this context.
 */
class RdContext<T : Any>(val key: String, val heavy: Boolean, val serializer: IMarshaller<T>) {
    companion object : ISerializer<RdContext<*>> {
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

        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): RdContext<*> {
            val keyId = buffer.readString()
            val isHeavy = buffer.readBoolean()
            val typeId = buffer.readRdId()

            @Suppress("UNCHECKED_CAST")
            return RdContext<Any>(keyId, isHeavy, ctx.serializers.get(typeId) as IMarshaller<Any>)
        }

        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: RdContext<*>) {
            buffer.writeString(value.key)
            buffer.writeBoolean(value.heavy)
            buffer.writeRdId(value.serializer.id)
        }
    }

    /**
     * The current (thread-local) value for this context
     */
    @Suppress("UNCHECKED_CAST")
    var value: T?
        get() = unsafeGet(key) as T?
        set(value) = unsafeSet(key, value)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if(other == null)
            return false
        if (this::class != other::class) return false

        other as RdContext<*>

        if (key != other.key) return false

        return true
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }
}