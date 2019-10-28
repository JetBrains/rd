package com.jetbrains.rd.framework.impl

import com.jetbrains.rd.framework.AbstractBuffer
import com.jetbrains.rd.framework.RdContext
import com.jetbrains.rd.framework.SerializationCtx
import com.jetbrains.rd.framework.base.IRdBindable
import com.jetbrains.rd.framework.base.ISingleContextHandler
import com.jetbrains.rd.framework.base.RdReactiveBase
import com.jetbrains.rd.util.ConcurrentHashMap
import com.jetbrains.rd.util.assert
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.AddRemove
import com.jetbrains.rd.util.reactive.IMutableViewableSet
import com.jetbrains.rd.util.reactive.ViewableSet
import com.jetbrains.rd.util.collections.ModificationCookieViewableSet
import com.jetbrains.rd.util.keySet

internal class HeavySingleContextHandler<T : Any>(override val context: RdContext<T>, private val contexts: ProtocolContexts) : RdReactiveBase(), ISingleContextHandler<T> {
    private val myProtocolValueSet = RdSet(context.serializer, ViewableSet(ConcurrentHashMap<T, Boolean>().keySet(true)))
    private val myProtocolValueSetWithCookie = ModificationCookieViewableSet(myProtocolValueSet, contexts::isWritingOwnMessages)

    override fun deepClone(): IRdBindable {
        error("This may not be cloned")
    }

    private inline fun withWriteOwnMessages(block: () -> Unit) {
        contexts.withWriteOwnMessages(block)
    }

    private val myInternRoot = InternRoot()

    val valueSet : IMutableViewableSet<T>
        get() = myProtocolValueSetWithCookie

    override fun init(lifetime: Lifetime) {
        super.init(lifetime)

        withWriteOwnMessages {
            myProtocolValueSet.rdid = rdid.mix("ValueSet")
            myProtocolValueSet.bind(lifetime, this, "ValueSet")

            myInternRoot.rdid = rdid.mix("InternRoot")
            myInternRoot.bind(lifetime, this, "InternRoot")
        }

        myProtocolValueSet.advise(lifetime) { addRemove, value ->
            handleValueAddedToProtocolSet(addRemove, value)
        }
    }

    private fun handleValueAddedToProtocolSet(addRemove: AddRemove, value: T) {
        withWriteOwnMessages {
            if (addRemove == AddRemove.Add) {
                myInternRoot.internValue(value)
            } else if (addRemove == AddRemove.Remove) {
                myInternRoot.removeValue(value)
            }
        }
    }

    override fun onWireReceived(buffer: AbstractBuffer) {
        error("SingleKeyContextHandler does not receive own messages")
    }

    @Suppress("UNCHECKED_CAST")
    override fun writeValue(ctx: SerializationCtx, buffer: AbstractBuffer) {
        assert(!contexts.isWritingOwnMessages) { "Trying to write context with a context-related message, key ${context.key}"}
        val value = context.value
        if(value == null) {
            buffer.writeInterningId(InterningId.invalid)
            buffer.writeBoolean(false)
        } else {
            withWriteOwnMessages {
                if (!myProtocolValueSet.contains(value)) {
                    if (protocol.scheduler.isActive) {
                        myProtocolValueSet.add(value)
                    } else error("Attempting to use previously unused context value $value on a background thread for key ${context.key}")
                }

                val internedId = myInternRoot.internValue(value)
                buffer.writeInterningId(internedId)
                if (!internedId.isValid) {
                    buffer.writeBoolean(true)
                    context.serializer.write(ctx, buffer, value)
                }
            }
        }
    }

    override fun readValue(ctx: SerializationCtx, buffer: AbstractBuffer): T? {
        val id = buffer.readInterningId()
        return if (!id.isValid) {
            val hasValue = buffer.readBoolean()
            if(hasValue)
                context.serializer.read(ctx, buffer)
            else
                null
        } else
            myInternRoot.tryUnInternValue(id)
    }

    override var async: Boolean
        get() = true
        set(value) = error("SingleKeyContextHandler is always async")

    override val wireScheduler = InternScheduler()
}