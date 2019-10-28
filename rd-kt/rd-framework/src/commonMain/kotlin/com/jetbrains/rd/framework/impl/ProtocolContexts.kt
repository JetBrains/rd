package com.jetbrains.rd.framework.impl

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.IRdBindable
import com.jetbrains.rd.framework.base.ISingleContextHandler
import com.jetbrains.rd.framework.base.RdReactiveBase
import com.jetbrains.rd.util.ConcurrentHashMap
import com.jetbrains.rd.util.CopyOnWriteArrayList
import com.jetbrains.rd.util.Sync
import com.jetbrains.rd.util.assert
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IMutableViewableSet
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.reactive.ViewableList
import com.jetbrains.rd.util.reflection.threadLocal
import com.jetbrains.rd.util.reflection.usingValue

/**
 * This class handles RdContext on protocol level. It tracks existing context keys and allows access to their value sets (when present)
 */
class ProtocolContexts(val serializationCtx: SerializationCtx) : RdReactiveBase() {
    private val counterpartHandlers = CopyOnWriteArrayList<ISingleContextHandler<*>>()
    private val myHandlerOrder = ViewableList(CopyOnWriteArrayList<ISingleContextHandler<*>>())
    private val handlersMap = ConcurrentHashMap<RdContext<*>, ISingleContextHandler<*>>()
    private val myOrderingsLock = Any() // used to protect writes to myKeyHandlersOrder

    internal var isWritingOwnMessages by threadLocal { false }
    internal inline fun withWriteOwnMessages(block: () -> Unit) = this::isWritingOwnMessages.usingValue(true, block)

    override fun deepClone(): IRdBindable {
        error("This may not be cloned")
    }

    override fun init(lifetime: Lifetime) {
        super.init(lifetime)
        wire.advise(lifetime, this)
        Sync.lock(myOrderingsLock) {
            myHandlerOrder.view(lifetime) { handlerLifetime, _, handler ->
                bindHandler(handlerLifetime, handler, handler.context.key)
                sendContextToRemote(handler.context)
            }
        }
    }

    override fun onWireReceived(buffer: AbstractBuffer) {
        assertWireThread()
        val context = RdContext.read(serializationCtx, buffer)
        if(context.heavy) {
            ensureContextHandlerExists(context)
        } else {
            @Suppress("UNCHECKED_CAST")
            createLightHandler(context as RdContext<Any>)
        }
        counterpartHandlers.add(handlersMap[context]!!)
    }

    private inline fun doAddHandler(context: RdContext<*>, factory: () -> ISingleContextHandler<*>) {
        assert(wireScheduler.isActive || protocol.scheduler.isActive)
        val value = factory()
        val prevValue = handlersMap.putIfAbsent(context, value)
        if (prevValue == null) {
            Sync.lock(myOrderingsLock) {
                myHandlerOrder.add(value)
            }
        }
    }

    private fun createLightHandler(context: RdContext<Any>) {
        assertWireThread() // can only happen on wire thread
        if (!handlersMap.containsKey(context)) {
            doAddHandler(context) { LightSingleContextHandler(context, context.serializer) }
        }
    }

    private fun <T : Any> ensureContextHandlerExists(context: RdContext<T>) {
        if (!handlersMap.containsKey(context)) {
            doAddHandler(context) {
                if(context.heavy)
                    HeavySingleContextHandler(context, this)
                else
                    LightSingleContextHandler(context, context.serializer)
            }
        }
    }

    private fun bindHandler(it: Lifetime, handler: ISingleContextHandler<*>, key: String) {
        if (handler !is HeavySingleContextHandler<*>) return
        handler.rdid = rdid.mix(key)
        protocol.scheduler.invokeOrQueue {
            withWriteOwnMessages {
                handler.bind(it, this, key)
            }
        }
    }

    private fun assertWireThread() {
        assert(wireScheduler.isActive) { "Must handle this on protocol thread" }
    }

    @Suppress("UNCHECKED_CAST")
    internal fun <T : Any> getContextHandler(context: RdContext<T>) : ISingleContextHandler<T> {
        return handlersMap[context] as ISingleContextHandler<T>
    }

    /**
     * Get a value set for a given context. The values are local relative to transform
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getValueSet(context: RdContext<T>) : IMutableViewableSet<T> {
        assert(context.heavy) { "Only heavy contexts have value sets, ${context.key} is not heavy" }
        return (getContextHandler(context) as HeavySingleContextHandler<T>).valueSet
    }

    /**
     * Registers a context to be used with this protocol. Must be invoked on protocol's scheduler
     */
    fun <T : Any> registerContext(context: RdContext<T>) {
        protocol.scheduler.assertThread()
        ensureContextHandlerExists(context)
    }

    private fun sendContextToRemote(context: RdContext<*>) {
        withWriteOwnMessages {
            wire.send(rdid) { buffer ->
                RdContext.write(serializationCtx, buffer, context)
            }
        }
    }

    /**
     * Writes the current context values to the given buffer
     */
    fun writeCurrentMessageContext(buffer: AbstractBuffer) {
        if (isWritingOwnMessages) return writeContextStub(buffer)

        val writtenSize = myHandlerOrder.size
        buffer.writeShort(writtenSize.toShort())
        for(i in 0 until writtenSize) {
            myHandlerOrder[i].writeValue(serializationCtx, buffer)
        }
    }

    /**
     * Reads a context from a given buffer and invokes the provided action in it
     */
    fun readMessageContextAndInvoke(buffer: AbstractBuffer, action: () -> Unit) {
        val numContextValues = buffer.readShort().toInt()
        assert(counterpartHandlers.size >= numContextValues) { "We know of ${counterpartHandlers.size} remote keys, received $numContextValues instead" }
        val oldValues = ArrayList<Any?>(numContextValues)
        for (i in 0 until numContextValues) {
            val otherSideHandler = counterpartHandlers[i]
            oldValues.add(otherSideHandler.context.value)

            val value = otherSideHandler.readValue(serializationCtx, buffer)
            RdContext.unsafeSet(otherSideHandler.context.key, value)
        }

        try {
            action()
        } finally {
            for(i in 0 until numContextValues) {
                val value = oldValues[i]
                RdContext.unsafeSet(counterpartHandlers[i].context.key, value)
            }
        }
    }

    companion object {
        /**
         * Writes an empty context
         */
        fun writeContextStub(buffer: AbstractBuffer) {
            buffer.writeShort(0)
        }
    }


    override var async: Boolean
        get() = true
        set(value) = error("ProtocolContextHandler is always async")

    override val wireScheduler: IScheduler = InternScheduler()
}