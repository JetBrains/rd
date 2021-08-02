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
    private val protocolValueSet = RdSet(context.serializer, ViewableSet(ConcurrentHashMap<T, Boolean>().keySet(true)))
    private val protocolValueSetWithCookie = ModificationCookieViewableSet(protocolValueSet, contexts::isSendWithoutContexts)
    @Suppress("UNCHECKED_CAST")
    private val internRoot = InternRoot(context.serializer)

    override fun deepClone(): IRdBindable {
        error("This may not be cloned")
    }

    private fun sendWithoutContexts(block: () -> Unit) {
        contexts.sendWithoutContexts(block)
    }

    val valueSet : IMutableViewableSet<T>
        get() = protocolValueSetWithCookie

    override fun init(lifetime: Lifetime) {
        super.init(lifetime)

        assert(contexts.isSendWithoutContexts) { "Must bind context handler without sending contexts to prevent reentrancy" }

        protocolValueSet.rdid = rdid.mix("ValueSet")
        protocolValueSet.bind(lifetime, this, "ValueSet")

        internRoot.rdid = rdid.mix("InternRoot")
        internRoot.bind(lifetime, this, "InternRoot")

        protocolValueSet.advise(lifetime) { addRemove, value ->
            handleValueAddedToProtocolSet(addRemove, value)
        }
    }

    private fun handleValueAddedToProtocolSet(addRemove: AddRemove, value: T) {
        sendWithoutContexts {
            if (addRemove == AddRemove.Add) {
                internRoot.intern(value)
            } else if (addRemove == AddRemove.Remove) {
                internRoot.remove(value)
            }
        }
    }

    override fun onWireReceived(buffer: AbstractBuffer) {
        error("SingleKeyContextHandler does not receive own messages")
    }

    @Suppress("UNCHECKED_CAST")
    override fun writeValue(ctx: SerializationCtx, buffer: AbstractBuffer) {
        assert(!contexts.isSendWithoutContexts) { "Trying to write context with a context-related message, key ${context.key}"}
        val value = context.value
        if(value == null) {
            buffer.writeInternId(InternId.invalid)
            buffer.writeBoolean(false)
        } else {
            sendWithoutContexts {
                addCurrentValueToValueSet(value)

                val internedId = internRoot.intern(value)
                buffer.writeInternId(internedId)
                if (!internedId.isValid) {
                    buffer.writeBoolean(true)
                    context.serializer.write(ctx, buffer, value)
                }
            }
        }
    }

    override fun registerValueInValueSet() {
        val value = context.value ?: return

        sendWithoutContexts {
            addCurrentValueToValueSet(value)
        }
    }

    private fun addCurrentValueToValueSet(value: T) {
        assert(contexts.isSendWithoutContexts) { "Values must be added to value set only when sending without contexts" }

        if (!protocolValueSet.contains(value)) {
            require(protocol.scheduler.isActive) { "Attempting to use previously unused context value $value on a background thread for key ${context.key}" }
            protocolValueSet.add(value)
        }
    }

    override fun readValue(ctx: SerializationCtx, buffer: AbstractBuffer): T? {
        val id = buffer.readInternId()
        return if (!id.isValid) {
            val hasValue = buffer.readBoolean()
            if(hasValue)
                context.serializer.read(ctx, buffer)
            else
                null
        } else
            internRoot.tryUnIntern(id)
    }

    override var async: Boolean
        get() = true
        set(value) = error("SingleKeyContextHandler is always async")

    override val wireScheduler = InternScheduler()
}