package com.jetbrains.rd.framework.impl

import com.jetbrains.rd.framework.AbstractBuffer
import com.jetbrains.rd.framework.RdContextKey
import com.jetbrains.rd.framework.SerializationCtx
import com.jetbrains.rd.framework.base.IRdBindable
import com.jetbrains.rd.framework.base.ISingleKeyProtocolContextHandler
import com.jetbrains.rd.framework.base.RdReactiveBase
import com.jetbrains.rd.util.ConcurrentHashMap
import com.jetbrains.rd.util.assert
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.AddRemove
import com.jetbrains.rd.util.reactive.IMutableViewableSet
import com.jetbrains.rd.util.reactive.ViewableSet

internal class InterningSingleKeyContextHandler<T : Any>(override val key: RdContextKey<T>, private val contextHandler: ProtocolContextHandler) : RdReactiveBase(), ISingleKeyProtocolContextHandler<T> {
    private val myProtocolValueSet = RdSet<T>()
    private val myLocalValueSet = ViewableSet<T>()
    private val myValueConcurrentSet = ConcurrentHashMap<T, T>()
    private var _myValueTransformer: ContextValueTransformer<T>? = null
    override var myValueTransformer : ContextValueTransformer<T>?
        get() = _myValueTransformer
        set(value) {
            _myValueTransformer = value
            localChange {
                myLocalValueSet.clear()
                if (value != null) {
                    myProtocolValueSet.forEach { setValue ->
                        value(setValue, ContextValueTransformerDirection.ReadFromProtocol)?.let { myLocalValueSet.add(it) }
                    }
                } else {
                    myLocalValueSet.addAll(myProtocolValueSet)
                }
            }
        }

    override fun deepClone(): IRdBindable {
        error("This may not be cloned")
    }

    private inline fun withWriteOwnMessages(block: () -> Unit) {
        contextHandler.withWriteOwnMessages(block)
    }

    private val myInternRoot = InternRoot()

    val valueSet : IMutableViewableSet<T>
        get() = myLocalValueSet

    internal val protocolValueSet : IMutableViewableSet<T>
        get() = myProtocolValueSet

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

        myLocalValueSet.advise(lifetime) { addRemove, value ->
            if (isLocalChange)
                return@advise
            handleValueAddedToLocalSet(addRemove, value)
        }
    }

    private fun handleValueAddedToLocalSet(addRemove: AddRemove, value: T) {
        val transformer = myValueTransformer
        val newValue = if (transformer != null) {
            transformer(value, ContextValueTransformerDirection.WriteToProtocol)
        } else {
            value
        } ?: return
        withWriteOwnMessages {
            if (addRemove == AddRemove.Add && !myProtocolValueSet.contains(newValue)) {
                myProtocolValueSet.add(newValue)
            } else if (addRemove == AddRemove.Remove && myProtocolValueSet.contains(newValue)) {
                myProtocolValueSet.remove(newValue)
            }
        }
    }

    private fun handleValueAddedToProtocolSet(addRemove: AddRemove, value: T) {
        val transformer = myValueTransformer
        val newValue = if(transformer != null) {
            transformer(value, ContextValueTransformerDirection.ReadFromProtocol)
        } else {
            value
        }

        withWriteOwnMessages {
            if (addRemove == AddRemove.Add) {
                myValueConcurrentSet[value] = value
                myInternRoot.internValue(value)
                if (newValue != null)
                    myLocalValueSet.add(newValue)
            } else if (addRemove == AddRemove.Remove) {
                myInternRoot.removeValue(value)
                myValueConcurrentSet.remove(value)

                if (newValue != null)
                    myLocalValueSet.remove(newValue)
            }
        }
    }

    override fun onWireReceived(buffer: AbstractBuffer) {
        error("SingleKeyContextHandler does not receive own messages")
    }

    @Suppress("UNCHECKED_CAST")
    override fun writeValue(ctx: SerializationCtx, writer: AbstractBuffer) {
        assert(!contextHandler.isWritingOwnMessages) { "Trying to write context with a context-related message, key ${key.key}"}
        val originalValue = key.value
        val value = transformValueToProtocol(originalValue)
        if(value == null)
            writer.writeInt(-1)
        else {
            withWriteOwnMessages {
                if (!myValueConcurrentSet.containsKey(value)) {
                    if (protocol.scheduler.isActive) {
                        myLocalValueSet.add(originalValue ?: error("Can't perform an implicit add with null local context value for key ${key.key}"))
                    } else error("Attempting to use previously unused context value $value on a background thread for key ${key.key}")
                }
                writer.writeInt(myInternRoot.internValue(value))
            }
        }
    }

    override fun readValue(ctx: SerializationCtx, reader: AbstractBuffer): T? {
        val id = reader.readInt()
        if (id == -1)
            return null
        return transformValueFromProtocol(myInternRoot.unInternValue(id xor 1))
    }

    override var async: Boolean
        get() = true
        set(value) = error("SingleKeyContextHandler is always async")

    override val wireScheduler = InternScheduler()
}