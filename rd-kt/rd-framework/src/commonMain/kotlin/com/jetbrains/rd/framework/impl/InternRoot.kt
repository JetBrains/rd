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

    private val myItemsList = ConcurrentHashMap<Int, Any>()
    private val otherItemsList = ConcurrentHashMap<Int, Any>()
    private val inverseMap = ConcurrentHashMap<Any, InverseMapValue>()

    companion object {
        private fun <E : Enum<E>> AbstractBuffer.writeEnumCheap(value: Enum<E>) {
            writeByte(value.ordinal.toByte())
        }

        private inline fun <reified E : Enum<E>> AbstractBuffer.readEnumCheap() : E {
            return parseFromOrdinal(readByte().toInt())
        }
    }

    override fun tryGetInterned(value: Any): Int {
        return inverseMap[value]?.id ?: -1
    }

    override fun internValue(value: Any): Int {
        return inverseMap[value]?.id ?: run {
            var idx = 0
            protocol.wire.send(rdid) { writer ->
                writer.writeEnumCheap(MessageType.SetIntern)
                Polymorphic.write(serializationContext, writer, value)
                Sync.lock(myItemsList) {
                    idx = myItemsList.size * 2
                    myItemsList[idx / 2] = value
                }
                writer.writeInt(idx)
            }
            val newValue = InverseMapValue(idx, -1)
            val oldValue = inverseMap.putIfAbsent(value, newValue)
            if (oldValue != null) { // if there was a value, then currently allocated index was extraneously sent to remote
                sendEraseIndex(idx) // tell the remote to disregard that
            } else { // otherwise, tell the remote that current index is the one that will be used for writes on this side
                sendConfirmIndex(idx)
            }
            oldValue?.id ?: idx
        }
    }

    private fun sendEraseIndex(index: Int) {
        require(isIndexOwned(index))
        protocol.wire.send(rdid) { writer ->
            writer.writeEnumCheap(MessageType.ResetIndex)
            writer.writeInt(index)
        }
        myItemsList.remove(index / 2)
    }

    private fun sendConfirmIndex(index: Int) {
        require(isIndexOwned(index))
        protocol.wire.send(rdid) { writer ->
            writer.writeEnumCheap(MessageType.ConfirmIndex)
            writer.writeInt(index)
        }
    }

    private fun eraseIndex(idx: Int) {
        if (isIndexOwned(idx))
            myItemsList.remove(idx / 2)
        else
            otherItemsList.remove(idx / 2)
    }

    override fun removeValue(value: Any) {
        val localValue = inverseMap.remove(value)
        if (localValue != null) {
            protocol.wire.send(rdid) { writer ->
                writer.writeEnumCheap(MessageType.RequestRemoval)
                writer.writeInt(localValue.id)
                writer.writeInt(localValue.extraId)
            }
        }
    }

    private fun getValue(id: Int) = if (isIndexOwned(id)) myItemsList[id / 2] else otherItemsList[id / 2]

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unInternValue(id: Int): T {
        val value = getValue(id)
        assert(value != null) { "Value for id $id has been removed" }
        return value as T
    }

    private fun isIndexOwned(id: Int) = (id and 1 == 0)

    override var async: Boolean
        get() = true
        set(_) = error("Intern Roots are always async")

    override val wireScheduler: IScheduler = InternScheduler()

    override fun onWireReceived(buffer: AbstractBuffer) {
        val messageType = buffer.readEnumCheap<MessageType>()
        return when(messageType) {
            MessageType.SetIntern -> handleSetInternMessage(buffer)
            MessageType.RequestRemoval -> handleRequestRemoval(buffer)
            MessageType.AckRemoval -> handleAckRemoval(buffer)
            MessageType.ResetIndex -> handleResetIndexMessage(buffer)
            MessageType.ConfirmIndex -> handleConfirmIndexMessage(buffer)
        }
    }

    private fun handleAckRemoval(buffer: AbstractBuffer) {
        val id1 = buffer.readInt() xor 1
        val id2 = buffer.readInt()
        eraseIndex(id1)
        if (id2 != -1) eraseIndex(id2 xor 1)
    }

    private fun handleRequestRemoval(buffer: AbstractBuffer) {
        val remoteId = buffer.readInt() xor 1
        val remoteExtraIdRaw = buffer.readInt()
        val remoteExtraId = remoteExtraIdRaw xor 1

        val value = getValue(remoteId)
        if (value != null) {
            val oldValue = inverseMap.remove(value)
            if (oldValue != null) {
                assert(oldValue.id == remoteId || oldValue.id == remoteExtraId)
                assert(oldValue.extraId == remoteId || oldValue.extraId == -1 || oldValue.extraId == remoteExtraId)
            }
            eraseIndex(remoteId)
            if(remoteExtraIdRaw != -1) eraseIndex(remoteExtraId)
        }
        sendRemovalAck(remoteId, if(remoteExtraIdRaw == -1) -1 else remoteExtraId)
    }

    private fun sendRemovalAck(id1: Int, id2: Int) {
        protocol.wire.send(rdid) { writer ->
            writer.writeEnumCheap(MessageType.AckRemoval)
            writer.writeInt(id1)
            writer.writeInt(id2)
        }
    }

    private fun handleResetIndexMessage(buffer: AbstractBuffer) {
        val remoteId = buffer.readInt()
        require(remoteId and 1 == 0) { "Remote sent ID marked as our own, bug?" }
        otherItemsList.remove(remoteId / 2)
    }

    private fun handleConfirmIndexMessage(buffer: AbstractBuffer) {
        val remoteId = buffer.readInt()
        require(remoteId and 1 == 0) { "Remote sent ID marked as our own, bug?" }
        val value = otherItemsList[remoteId / 2] ?: return // there's a chance that this ID was already removed on the other side before it could confirm it - ignore such cases
        inverseMap.putIfAbsent(value, InverseMapValue(remoteId xor 1, -1))?.let {
            assert(it.extraId == -1) { "extraId already set for entity $value with ids ${it.id}/${it.extraId}"}
            it.extraId = remoteId xor 1
        }
    }

    private fun handleSetInternMessage(buffer: AbstractBuffer) {
        val value = Polymorphic.read(serializationContext, buffer) ?: return
        val remoteId = buffer.readInt()
        require(remoteId and 1 == 0) { "Remote sent ID marked as our own, bug?" }
        otherItemsList[remoteId / 2] = value
    }

    private enum class MessageType {
        SetIntern,
        ResetIndex,
        ConfirmIndex,
        RequestRemoval,
        AckRemoval,
    }

    private data class InverseMapValue(val id: Int, var extraId: Int)


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

        myItemsList.clear()
        otherItemsList.clear()
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

    /**
     * Removal procedure:
     * remove() is invoked on local end. A removal request is sent, containing all known IDs for this value. Value is removed from inverse map and future interns use a new ID.
     * (at this point an in-progress message on a background thread could still contain the to-be-removed ID. It's users's fault that they removed an actively used value)
     * Remote end receives removal request. It removes the value from its own inverse map, its direct maps, and acknowledges removal
     * Local end receives ack and removes local values
     */
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