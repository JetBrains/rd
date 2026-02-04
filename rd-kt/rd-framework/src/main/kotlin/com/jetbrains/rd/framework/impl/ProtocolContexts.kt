package com.jetbrains.rd.framework.impl

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.framework.base.ISingleContextHandler
import com.jetbrains.rd.util.ConcurrentHashMap
import com.jetbrains.rd.util.CopyOnWriteArrayList
import com.jetbrains.rd.util.Sync
import com.jetbrains.rd.util.assert
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IAppendOnlyViewableConcurrentSet
import com.jetbrains.rd.util.reactive.ViewableList
import com.jetbrains.rd.util.reflection.threadLocal
import com.jetbrains.rd.util.reflection.usingValue

/**
 * This class handles RdContext on protocol level. It tracks existing contexts and allows access to their value sets (when present)
 */
class ProtocolContexts(val serializationCtx: SerializationCtx) : RdReactiveBase() {
    private val counterpartHandlers = CopyOnWriteArrayList<ISingleContextHandler<*>>()
    private val handlersToWrite = CopyOnWriteArrayList<ISingleContextHandler<*>>()
    private val myHandlerOrder = ViewableList<ISingleContextHandler<*>>()
    private val handlersMap = ConcurrentHashMap<RdContext<*>, ISingleContextHandler<*>>()
    private val myOrderingsLock = Any() // used to protect writes to myKeyHandlersOrder

    internal var isSendWithoutContexts by threadLocal { false }
    internal fun <T> sendWithoutContexts(block: () -> T) = this::isSendWithoutContexts.usingValue(true, block)

    override fun deepClone(): IRdBindable {
        error("This may not be cloned")
    }

    val registeredContexts: Collection<RdContext<*>>
        get() = handlersMap.keys

    override fun preInit(lifetime: Lifetime, proto: IProtocol) {
        super.preInit(lifetime, proto)

        Sync.lock(myOrderingsLock) {
            myHandlerOrder.view(lifetime) { handlerLifetime, _, handler ->
                preBindHandler(handlerLifetime, handler, handler.context.key, proto)
            }
        }

        proto.wire.advise(lifetime, this)
    }

    override fun init(lifetime: Lifetime, proto: IProtocol, ctx: SerializationCtx) {
        super.init(lifetime, proto, ctx)
        Sync.lock(myOrderingsLock) {
            myHandlerOrder.view(lifetime) { handlerLifetime, _, handler ->
                bindHandler(handler)
                sendContextToRemote(handler.context)

                // add the handler to myHandlersToWrite only after sending the context to remote
                handlersToWrite.add(handler)
            }
        }
    }

    override fun onWireReceived(proto: IProtocol, buffer: AbstractBuffer, ctx: SerializationCtx, dispatchHelper: IRdWireableDispatchHelper) {
        val context = RdContext.read(serializationCtx, buffer)
        ensureContextHandlerExists(context)
        counterpartHandlers.add(handlersMap[context]!!)
    }

    private inline fun doAddHandler(context: RdContext<*>, factory: () -> ISingleContextHandler<*>) {
        val value = factory()
        val prevValue = handlersMap.putIfAbsent(context, value)
        if (prevValue == null) {
            Sync.lock(myOrderingsLock) {
                myHandlerOrder.add(value)
            }
        }
    }

    private fun <T : Any> ensureContextHandlerExists(context: RdContext<T>) {
        if (!handlersMap.containsKey(context)) {
            serializationCtx.serializers.register(RdContext.marshallerFor(context))
            doAddHandler(context) {
                if(context.heavy)
                    HeavySingleContextHandler(context, this)
                else
                    LightSingleContextHandler(context)
            }
        }
    }

    private fun preBindHandler(lifetime: Lifetime, handler: ISingleContextHandler<*>, key: String, proto: IProtocol) {
        if (handler !is HeavySingleContextHandler<*>) return

        handler.rdid = proto.identity.mix(rdid, key)
        handler.preBind(lifetime, this, key)
    }

    private fun bindHandler(handler: ISingleContextHandler<*>) {
        if (handler !is HeavySingleContextHandler<*>) return

        sendWithoutContexts {
            handler.bind()
        }
    }

    @Suppress("UNCHECKED_CAST")
    internal fun <T : Any> getContextHandler(context: RdContext<T>) : ISingleContextHandler<T> {
        return handlersMap[context] as ISingleContextHandler<T>
    }

    /**
     * Get a value set for a given context
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getValueSet(context: RdContext<T>) : IAppendOnlyViewableConcurrentSet<T> {
        assert(context.heavy) { "Only heavy contexts have value sets, ${context.key} is not heavy" }
        return (getContextHandler(context) as HeavySingleContextHandler<T>).valueSet
    }

    /**
     * Registers a context to be used with this protocol. Must be invoked on protocol's scheduler
     */
    fun <T : Any> registerContext(context: RdContext<T>) {
        ensureContextHandlerExists(context)
    }

    private fun sendContextToRemote(context: RdContext<*>) {
        val proto = protocol ?: return

        sendWithoutContexts {
            proto.wire.send(rdid) { buffer ->
                RdContext.write(serializationCtx, buffer, context)
            }
        }
    }

    /**
     * Writes the current context values to the given buffer
     */
    fun writeCurrentMessageContext(buffer: AbstractBuffer) {
        if (isSendWithoutContexts) return writeEmptyContexts(buffer)

        // all handlers in handlersToWrite have been sent to the remote side
        val writtenSize = handlersToWrite.size
        buffer.writeShort(writtenSize.toShort())
        for (i in 0 until writtenSize) {
            handlersToWrite[i].writeValue(serializationCtx, buffer)
        }
    }

    internal fun readContext(buffer: AbstractBuffer): MessageContext {
        val numContextValues = buffer.readShort().toInt()
        val values = arrayOfNulls<Any>(numContextValues)
        val handlers = arrayOfNulls<ISingleContextHandler<*>>(numContextValues)
        assert(counterpartHandlers.size >= numContextValues) { "We know of ${counterpartHandlers.size} remote keys, received $numContextValues instead" }

        for (i in 0 until numContextValues) {
            val handler = counterpartHandlers[i]
            values[i] = handler.readValue(serializationCtx, buffer)
            handlers[i] = handler
        }

        return MessageContext(values as Array<Any>, handlers as Array<ISingleContextHandler<*>>)
    }

    internal class MessageContext(private val values: Array<Any>, private val handlers: Array<ISingleContextHandler<*>>) {
        inline fun update(action: () -> Unit) {
            val disposables = arrayOfNulls<AutoCloseable>(values.size)

            for (i in values.indices) {
                val handler = handlers[i].context as RdContext<Any>
                disposables[i] = handler.updateValue(values[i])
            }

            try {
                action()
            } finally {
                for (value in disposables) {
                    value?.close()
                }
            }
        }
    }

    companion object {
        /**
         * Writes an empty context
         */
        fun writeEmptyContexts(buffer: AbstractBuffer) {
            buffer.writeShort(0)
        }
    }

    override var async: Boolean
        get() = true
        set(value) = error("ProtocolContextHandler is always async")
}