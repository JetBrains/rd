package com.jetbrains.rd.framework

import com.jetbrains.rd.util.threadLocalWithInitial
import kotlin.reflect.KClass

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
abstract class RdContext<T : Any>(val key: String, val heavy: Boolean, val serializer: IMarshaller<T>) {
    private val internalValue = threadLocalWithInitial<T?> { null }
    companion object : ISerializer<RdContext<*>> {
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): RdContext<*> {
            return ctx.serializers.readPolymorphic(ctx, buffer)
        }

        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: RdContext<*>) {
            ctx.serializers.writePolymorphic(ctx, buffer, value)
        }

        fun marshallerFor(context: RdContext<*>): IMarshaller<RdContext<*>> {
            return object : IMarshaller<RdContext<*>> {
                override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): RdContext<*> {
                    return context
                }

                override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: RdContext<*>) {
                    // noop write
                }

                override val id: RdId
                    get() = RdId("RdContext-${context.key}".getPlatformIndependentHash())

                override val _type: KClass<*>
                    get() = context::class
            }
        }
    }

    /**
     * The current (thread-local) value for this context
     */
    @Suppress("UNCHECKED_CAST")
    var value: T?
        get() = internalValue.get()
        set(value) = internalValue.set(value)

    /**
     * Value which is used as a key inside per-context entities like [RdPerContextMap][com.jetbrains.rd.framework.impl.RdPerContextMap]
     */
    open var valueForPerContextEntity: T?
        get() = value
        set(value) { this.value = value }

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