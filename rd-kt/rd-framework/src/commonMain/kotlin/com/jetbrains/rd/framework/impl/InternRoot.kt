package com.jetbrains.rd.framework.impl

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.util.*
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.string.RName
import com.jetbrains.rd.framework.IInternRoot
import com.jetbrains.rd.framework.base.IRdBindable

class InternRoot: IInternRoot {
    override fun deepClone(): IRdBindable {
        error("Should never be called")
    }

    private val internedValueIndices = AtomicInteger()
    private val directMap = ConcurrentHashMap<InterningId, Any>()
    private val inverseMap = ConcurrentHashMap<Any, InverseMapValue>()

    override fun tryGetInterned(value: Any): InterningId {
        return inverseMap[value]?.id ?: InterningId.invalid
    }

    override fun internValue(value: Any): InterningId {
        return inverseMap[value]?.id ?: run {
            val idx = InterningId(internedValueIndices.incrementAndGet() * 2)
            assert(idx.isLocal)
            val newValue = InverseMapValue(InterningId.invalid, InterningId.invalid)
            val oldValue = inverseMap.putIfAbsent(value, newValue)
            if (oldValue != null) { // someone already allocated inverse mapping for this value
                oldValue.id
            } else { // we've succeeded at allocating this value - send it
                directMap[idx] = value
                protocol.wire.send(rdid) { writer ->
                    Polymorphic.write(serializationContext, writer, value)
                    writer.writeInterningId(idx)
                }
                newValue.id = idx
                idx
            }
        }
    }

    override fun removeValue(value: Any) {
        val localValue = inverseMap.remove(value)
        if (localValue != null) {
            directMap.remove(localValue.id)
            directMap.remove(localValue.extraId)
        }
    }

    private fun getValue(id: InterningId): Any? {
        assert(id.isValid)
        return directMap[id]
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unInternValue(id: InterningId): T {
        val value = getValue(id)
        require(value != null) { "Value for id $id has been removed" }
        return value as T
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> tryUnInternValue(id: InterningId): T? {
        return getValue(id) as T?
    }

    override var async: Boolean
        get() = true
        set(_) = error("Intern Roots are always async")

    override val wireScheduler: IScheduler = InternScheduler()

    override fun onWireReceived(buffer: AbstractBuffer) {
        val value = Polymorphic.read(serializationContext, buffer) ?: return
        val remoteId = buffer.readInterningId()
        assert(!remoteId.isLocal) { "Remote sent local InterningId, bug?" }
        assert(remoteId.isValid)
        directMap[remoteId] = value
        val newInverseMapValue = InverseMapValue(remoteId, InterningId.invalid)
        val oldValue = inverseMap.putIfAbsent(value, newInverseMapValue)
        if (oldValue != null) {
            oldValue.extraId = remoteId
        }
    }

    private data class InverseMapValue(/*@Volatile*/ var id: InterningId, var extraId: InterningId) // @Volatile is broken when compiling js, actual/expected with it breaks when compiling jvm

    override var rdid: RdId = RdId.Null
        internal set

    override fun bind(lf: Lifetime, parent: IRdDynamic, name: String) {
        require (this.parent == null) { "Trying to bound already bound $this to ${parent.location}" }

        lf.bracket({
            this.parent = parent
            location = parent.location.sub(name, ".")
        }, {
            location = location.sub("<<unbound>>", "::")
            this.parent = null
            rdid = RdId.Null
        })

        internedValueIndices.set(0)
        directMap.clear()
        inverseMap.clear()

        protocol.wire.advise(lf, this)
    }

    override fun identify(identities: IIdentities, id: RdId) {
        require(rdid.isNull) { "Already has RdId: $rdid, entity: $this" }
        require(!id.isNull) { "Assigned RdId mustn't be null, entity: $this" }

        rdid = id
    }

    var parent: IRdDynamic? = null

    override val protocol: IProtocol
        get() = parent?.protocol ?: error("Not bound: $location")

    override val serializationContext: SerializationCtx
        get() = parent?.serializationContext ?: error("Not bound: $location")

    override var location: RName = RName("<<not bound>>")
}

inline class InterningId(val value: Int) {
    val isValid: Boolean
        get() = value != -1

    val isLocal: Boolean
        get() = (value and 1) == 0

    companion object {
        val invalid = InterningId(-1)
    }
}

fun AbstractBuffer.readInterningId(): InterningId {
    return InterningId(readInt())
}

fun AbstractBuffer.writeInterningId(id: InterningId) {
    writeInt(if(id.value == -1) id.value else id.value xor 1)
}

class InternScheduler : IScheduler {
    override fun queue(action: () -> Unit) {
        activeCounts.set(activeCounts.get() + 1)
        try {
            action()
        } finally {
            activeCounts.set(activeCounts.get() - 1)
        }
    }
    override val isActive: Boolean
        get() = activeCounts.get() > 0
    override fun flush() = Unit
    override val outOfOrderExecution: Boolean
        get() = true

    private val activeCounts = threadLocalWithInitial { 0 }
}