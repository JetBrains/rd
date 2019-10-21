package com.jetbrains.rd.framework.impl

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.IRdBindable
import com.jetbrains.rd.framework.base.ISingleKeyProtocolContextHandler
import com.jetbrains.rd.framework.base.RdReactiveBase
import com.jetbrains.rd.util.ConcurrentHashMap
import com.jetbrains.rd.util.CopyOnWriteArrayList
import com.jetbrains.rd.util.Sync
import com.jetbrains.rd.util.assert
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.onTermination
import com.jetbrains.rd.util.reactive.IMutableViewableSet
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.reflection.threadLocal

/**
 * A callback to transform values between protocol and local values. Must be a bijection (one-to-one map)
 */
typealias ContextValueTransformer<T> = (value: T?, fromProtocol: ContextValueTransformerDirection) -> T?

/**
 * Indicates transformation direction for a value transformer
 */
enum class ContextValueTransformerDirection {
    WriteToProtocol,
    ReadFromProtocol
}

/**
 * This class handles RdContext on protocol level. It tracks existing context keys and allows access to their value sets (when present)
 */
class ProtocolContextHandler(val serializationCtx: SerializationCtx) : RdReactiveBase() {
    private val otherSideKeys = CopyOnWriteArrayList<String>()
    private val myKeyHandlerOrdering = CopyOnWriteArrayList<ISingleKeyProtocolContextHandler<*>>()
    private val myKeyHandlers = ConcurrentHashMap<String, ISingleKeyProtocolContextHandler<*>>()
    private var myBindLifetime: Lifetime? = null
    private val myOrderingsLock = Any()

    internal var isWritingOwnMessages by threadLocal { false }
    internal inline fun withWriteOwnMessages(block: () -> Unit) {
        val oldValue = isWritingOwnMessages
        isWritingOwnMessages = true
        try {
            block()
        } finally {
            isWritingOwnMessages = oldValue
        }
    }

    override fun deepClone(): IRdBindable {
        error("This may not be cloned")
    }

    override fun init(lifetime: Lifetime) {
        super.init(lifetime)
        wire.advise(lifetime, this)
        Sync.lock(myOrderingsLock) {
            myKeyHandlerOrdering.forEach {
                bindHandler(lifetime, it, it.key.key)
                sendKeyToRemote(it.key)
            }
        }

        myBindLifetime = lifetime
        lifetime.onTermination {
            myBindLifetime = null
        }
    }

    override fun onWireReceived(buffer: AbstractBuffer) {
        assertWireThread()
        val keyId = buffer.readString()
        val isHeavy = buffer.readBoolean()
        val typeId = buffer.readRdId()
        otherSideKeys.add(keyId)
        @Suppress("UNCHECKED_CAST") val szr = serializationCtx.serializers.get(typeId) as IMarshaller<Any>
        if(isHeavy) {
            ensureKeyHandlerExists(RdContextKey(keyId, true, szr))
        } else {
            createSimpleHandlerWithTypeId(keyId, szr)
        }
    }

    private inline fun doAddHandler(key: String, factory: () -> ISingleKeyProtocolContextHandler<*>) {
        assert(wireScheduler.isActive || protocol.scheduler.isActive)
        val value = factory()
        val prevValue = myKeyHandlers.putIfAbsent(key, value)
        if (prevValue == null) { // null or stub
            Sync.lock(this) {
                sendKeyToRemote(value.key)
                myKeyHandlerOrdering.add(value)
            }
            myBindLifetime?.let {
                bindHandler(it, value, key)
            }
        }
    }

    private fun createSimpleHandlerWithTypeId(key: String, szr: IMarshaller<Any>) {
        assertWireThread() // can only happen on wire thread
        if (!myKeyHandlers.containsKey(key)) {
            doAddHandler(key) { SimpleSingleKeyContextHandler(RdContextKey<Any>(key, false, szr), szr) }
        }
    }

    private fun <T : Any> ensureKeyHandlerExists(key: RdContextKey<T>) {
        if (!myKeyHandlers.containsKey(key.key)) {
            doAddHandler(key.key) {
                if(key.heavy)
                    InterningSingleKeyContextHandler(key, this)
                else
                    SimpleSingleKeyContextHandler(key, key.lightSerializer)
            }
        }
    }

    private fun bindHandler(it: Lifetime, handler: ISingleKeyProtocolContextHandler<*>, key: String) {
        if (handler !is InterningSingleKeyContextHandler<*>) return
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
    internal fun <T : Any> getKeyHandler(key: RdContextKey<T>) : ISingleKeyProtocolContextHandler<T> {
        require(myKeyHandlers.containsKey(key.key))
        return myKeyHandlers[key.key]!! as ISingleKeyProtocolContextHandler<T>
    }

    /**
     * Get a value set for a given key. The values are local relative to transform
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getValueSet(key: RdContextKey<T>) : IMutableViewableSet<T> {
        assert(key.heavy) { "Only heavy contexts have value sets, ${key.key} is not heavy" }
        return (getKeyHandler(key) as InterningSingleKeyContextHandler<T>).valueSet
    }

    /**
     * Gets a value set for a given key. The values are what actually exists on protocol level
     */
    @Suppress("UNCHECKED_CAST")
    internal fun <T : Any> getProtocolValueSet(key: RdContextKey<T>) : IMutableViewableSet<T> {
        assert(key.heavy) { "Only heavy contexts have value sets, ${key.key} is not heavy" }
        return (getKeyHandler(key) as InterningSingleKeyContextHandler<T>).protocolValueSet
    }

    /**
     * Registers a context key to be used with this context handler. Must be invoked on protocol's scheduler
     */
    fun <T : Any> registerKey(key: RdContextKey<T>) {
        protocol.scheduler.assertThread()
        ensureKeyHandlerExists(key)
    }

    /**
     * Sets a transform for a given key. The transform must be a bijection (one-to-one map). This will regenerate the local value set based on the protocol value set
     */
    fun <T:Any> setTransformerForKey(key: RdContextKey<T>, transformer: ContextValueTransformer<T>?) {
        getKeyHandler(key).myValueTransformer = transformer
    }

    private fun sendKeyToRemote(key: RdContextKey<*>) {
        withWriteOwnMessages {
            wire.send(rdid) { writer ->
                writer.writeString(key.key) // todo: send id for validation?
                writer.writeBoolean(key.heavy)
                writer.writeRdId(key.lightSerializer.id)
            }
        }
    }

    /**
     * Writes the current context values to the given buffer
     */
    fun writeCurrentMessageContext(writer: AbstractBuffer) {
        if (isWritingOwnMessages) return writeContextStub(writer)

        val writtenSize = myKeyHandlerOrdering.size
        writer.writeShort(writtenSize.toShort())
        for(i in 0 until writtenSize) {
            myKeyHandlerOrdering[i].writeValue(serializationCtx, writer)
        }
    }

    /**
     * Reads a context from a given buffer and invokes the provided action in it
     */
    fun readMessageContextAndInvoke(reader: AbstractBuffer, action: () -> Unit) {
        val numContextValues = reader.readShort().toInt()
        assert(otherSideKeys.size >= numContextValues) { "We know of ${otherSideKeys.size} remote keys, received $numContextValues instead" }
        val oldValues = ArrayList<Any?>(numContextValues)
        for (i in 0 until numContextValues) {
            val key = otherSideKeys[i]
            oldValues.add(RdContextKey.unsafeGet(key))

            val value = myKeyHandlers[key]!!.readValue(serializationCtx, reader)
            RdContextKey.unsafeSet(key, value)
        }

        try {
            action()
        } finally {
            for(i in 0 until numContextValues) {
                val value = oldValues[i]
                RdContextKey.unsafeSet(otherSideKeys[i], value)
            }
        }
    }

    companion object {
        /**
         * Writes an empty context
         */
        fun writeContextStub(writer: AbstractBuffer) {
            writer.writeShort(0)
        }
    }


    override var async: Boolean
        get() = true
        set(value) = error("ProtocolContextHandler is always async")

    override val wireScheduler: IScheduler = InternScheduler()
}