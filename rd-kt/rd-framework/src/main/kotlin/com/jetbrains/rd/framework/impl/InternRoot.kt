package com.jetbrains.rd.framework.impl

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.IRdBindable
import com.jetbrains.rd.framework.base.IRdWireableDispatchHelper
import com.jetbrains.rd.framework.base.RdReactiveBase
import com.jetbrains.rd.util.assert
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.string.RName
import com.jetbrains.rd.util.trace
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.set

class InternRoot<TBase: Any>(val serializer: ISerializer<TBase> = Polymorphic()): IInternRoot<TBase> {
    override fun deepClone(): IRdBindable {
        error("Should never be called")
    }

    private val internedValueIndices = AtomicInteger()
    private val directMap = ConcurrentHashMap<InternId, TBase>()
    private val inverseMap = ConcurrentHashMap<TBase, InverseMapValue>()

    override fun tryGetInterned(value: TBase): InternId {
        return inverseMap[value]?.id ?: InternId.invalid
    }

    override fun intern(value: TBase): InternId {
        val proto = protocol ?: return InternId.invalid
        val ctx = serializationContext ?: return InternId.invalid

        return inverseMap[value]?.id ?: run {
            val newMapping = InverseMapValue(InternId.invalid, InternId.invalid)
            val oldMapping = inverseMap.putIfAbsent(value, newMapping)
            if (oldMapping != null) { // someone already allocated inverse mapping for this value
                oldMapping.id
            } else { // we've succeeded at allocating this value - send it
                val idx = InternId(internedValueIndices.incrementAndGet() * 2)
                assert(idx.isLocal)

                RdReactiveBase.logSend.trace { "InternRoot `$location` ($rdid):: $idx = $value" }

                directMap[idx] = value
                proto.contexts.sendWithoutContexts {
                    proto.wire.send(rdid) { writer ->
                        serializer.write(ctx, writer, value)
                        writer.writeInternId(idx)
                    }
                }
                newMapping.id = idx
                idx
            }
        }
    }

    override fun remove(value: TBase) {
        val localValue = inverseMap.remove(value)
        if (localValue != null) {
            directMap.remove(localValue.id)
            directMap.remove(localValue.extraId)
        }
    }

    private fun get(id: InternId): TBase? {
        assert(id.isValid)
        return directMap[id]
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : TBase> unIntern(id: InternId): T {
        val value = get(id)
        require(value != null) { "Value for id $id has been removed" }
        return value as T
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : TBase> tryUnIntern(id: InternId): T? {
        return get(id) as T?
    }

    override var async: Boolean
        get() = true
        set(_) = error("Intern Roots are always async")

    override fun onWireReceived(buffer: AbstractBuffer, dispatchHelper: IRdWireableDispatchHelper) {
        val ctx = serializationContext
        if (ctx == null) {
            RdReactiveBase.logReceived.trace { "$this is not bound. Message for (${dispatchHelper.rdId} will not be processed" }
            return
        }

        val value = serializer.read(ctx, buffer)

        val remoteId = buffer.readInternId()
        assert(!remoteId.isLocal) { "Remote sent local InterningId, bug?" }
        assert(remoteId.isValid) { "Remote sent invalid InterningId, bug?" }
        RdReactiveBase.logReceived.trace { "InternRoot `$location` ($rdid):: $remoteId = $value" }
        directMap[remoteId] = value
        val newInverseMapValue = InverseMapValue(remoteId, InternId.invalid)
        val oldValue = inverseMap.putIfAbsent(value, newInverseMapValue)
        if (oldValue != null) {
            assert(!oldValue.extraId.isValid) { "Remote send duplicate IDs for value $value" }
            oldValue.extraId = remoteId
        }
    }

    private data class InverseMapValue(/*@Volatile*/ var id: InternId, var extraId: InternId) // @Volatile is broken when compiling js, actual/expected with it breaks when compiling jvm

    override var rdid: RdId = RdId.Null
        internal set

    override fun preBind(lf: Lifetime, parent: IRdDynamic, name: String) {
        require (this.parent == null) { "Trying to bound already bound $this to ${parent.location}" }

        val proto = parent.protocol ?: return

        lf.bracketIfAlive({
            this.parent = parent
            location = parent.location.sub(name, ".")

            directMap.clear()
            inverseMap.clear()
        }, {
            location = location.sub("<<unbound>>", "::")
            this.parent = null
            rdid = RdId.Null
        })

        proto.wire.advise(lf, this)
    }

    override fun bind() {

    }

    override fun identify(identities: IIdentities, id: RdId) {
        require(rdid.isNull) { "Already has RdId: $rdid, entity: $this" }
        require(!id.isNull) { "Assigned RdId mustn't be null, entity: $this" }

        rdid = id
    }

    var parent: IRdDynamic? = null

    override val protocol: IProtocol?
        get() = parent?.protocol

    override val serializationContext: SerializationCtx?
        get() = parent?.serializationContext ?: error("Not bound: $location")

    override var location: RName = RName("<<not bound>>")
}

/**
 * An ID representing an interned value
 */
@Suppress("EXPERIMENTAL_FEATURE_WARNING")
inline class InternId(val value: Int) {
    /**
     * True if this ID represents an actual interned value. False indicates a failed interning operation or unset value
     */
    val isValid: Boolean
        get() = value != -1

    /**
     * True if this ID represents a value interned by local InternRoot
     */
    val isLocal: Boolean
        get() = (value and 1) == 0

    companion object {
        val invalid = InternId(-1)
    }

    override fun toString() = "InternId($value)"
}

fun AbstractBuffer.readInternId(): InternId {
    return InternId(readInt())
}

fun AbstractBuffer.writeInternId(id: InternId) {
    writeInt(if(id.value == -1) id.value else id.value xor 1)
}