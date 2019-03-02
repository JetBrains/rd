package com.jetbrains.rd.framework.impl

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.util.*
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.string.RName
import com.jetbrains.rd.framework.IInternRoot

class InternRoot: IInternRoot {
    private val myItemsList = ArrayList<Any>()
    private val otherItemsList = ConcurrentHashMap<Int, Any>()
    private val inverseMap = ConcurrentHashMap<Any, Int>()

    override fun tryGetInterned(value: Any): Int {
        return inverseMap[value] ?: -1
    }

    override fun internValue(value: Any): Int {
        return inverseMap[value] ?: run {
            var idx = 0
            protocol.wire.send(rdid) { writer ->
                Polymorphic.write(serializationContext, writer, value)
                Sync.lock(myItemsList) {
                    idx = myItemsList.size * 2
                    myItemsList.add(value)
                }
                writer.writeInt(idx)
            }
            inverseMap.putIfAbsent(value, idx) ?: idx
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unInternValue(id: Int): T {
        return (if (isIndexOwned(id)) myItemsList[id / 2] else otherItemsList[id / 2]) as T
    }

    private fun isIndexOwned(id: Int) = (id and 1 == 0)

    override fun setInternedCorrespondence(id: Int, value: Any) {
        require(!isIndexOwned(id)) { "Setting interned correspondence for object that we should have written, bug?" }
//        require(id / 2 == otherItemsList.size, { "Out-of-sequence interned object id" })

        otherItemsList[id / 2] = value
        inverseMap[value] = id
    }

    override var async: Boolean
        get() = true
        set(value) = error("Intern Roots are always async")

    override val wireScheduler: IScheduler = InternScheduler()

    override fun onWireReceived(buffer: AbstractBuffer) {
        val value = Polymorphic.read(serializationContext, buffer) ?: return
        val remoteId = buffer.readInt()
        require(remoteId and 1 == 0) { "Remote sent ID marked as our own, bug?" }
        setInternedCorrespondence(remoteId xor 1, value)
    }

    override var rdid: RdId = RdId.Null
        get
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