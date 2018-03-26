package com.jetbrains.rider.framework.base

import com.jetbrains.rider.framework.*
import com.jetbrains.rider.framework.impl.RdPropertyBase
import com.jetbrains.rider.util.Logger
import com.jetbrains.rider.util.Sync
import com.jetbrains.rider.util.collections.QueueImpl
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.reactive.*
import com.jetbrains.rider.util.string.printToString
import com.jetbrains.rider.util.trace

abstract class RdExtBase : RdReactiveBase() {
    enum class ExtState {
        Ready,
        ReceivedCounterpart,
        Disconnected
    }


    private val extWire = ExtWire()
    private var extScheduler: ExtScheduler? = null
    private var extProtocol: IProtocol? = null
    val connected = extWire.connected


    override val protocol : IProtocol get() = extProtocol ?: super.protocol //nb

    abstract val serializersOwner : ISerializersOwner
    open val serializationHash : Long = 0L


    override fun init(lifetime: Lifetime) {
        Protocol.initializationLogger.traceMe { "binding" }

        val parentProtocol = super.protocol
        val parentWire = parentProtocol.wire

        serializersOwner.register(parentProtocol.serializers)

//        val sc = ExtScheduler(parentProtocol.scheduler)
        val sc = parentProtocol.scheduler
        lifetime.bracket(
            {
//                extScheduler = sc
                extProtocol = Protocol(parentProtocol.serializers, parentProtocol.identity, sc, extWire) },
            {
                extProtocol = null
//                extScheduler = null
            }
        )


        parentWire.advise(lifetime, rdid) { buffer ->
            val remoteState = buffer.readEnum<ExtState>()
            logReceived.traceMe { "remote: $remoteState " }

            @Suppress("REDUNDANT_ELSE_IN_WHEN")
            when (remoteState) {
                RdExtBase.ExtState.Ready -> {


                    parentWire.sendState(ExtState.ReceivedCounterpart)
                    extWire.connectedWire.set(parentWire)
                }
                RdExtBase.ExtState.ReceivedCounterpart -> extWire.connectedWire.set(parentWire) //don't set anything if already set
                RdExtBase.ExtState.Disconnected -> extWire.connectedWire.set(null)

                else -> error("Unknown remote state: $remoteState")
            }

            val counterpartSerializationHash = buffer.readLong()
            if (serializationHash != counterpartSerializationHash) {
                error("serializationHash of ext '${location()}' doesn't match to counterpart: maybe you forgot to generate models?")
            }
        }

        lifetime.bracket(
            { parentWire.sendState(ExtState.Ready) },
            { parentWire.sendState(ExtState.Disconnected) }
        )


        //todo make it smarter
        for ((name, child) in bindableChildren) {
            if (child is RdPropertyBase<*> && child.defaultValueChanged) {
                child.localChange {
                    child.bind(lifetime, this, name)
                }
            }
            else {
                child?.bindPolymorphic(lifetime, this, name)
            }
        }

        Protocol.initializationLogger.traceMe { "created and bound :: ${printToString()}" }
    }

    private fun IWire.sendState(state: ExtState) = send(rdid) {
        logSend.traceMe {state}
        it.writeEnum (state)
        it.writeLong(serializationHash)
    }
    private inline fun Logger.traceMe (message:() -> Any) = this.trace { "ext `${location()}` ($rdid) :: ${message()}" }

    fun pumpScheduler() = extScheduler?.pump()
}


//todo make it more efficient
class ExtScheduler(private val parentScheduler: IScheduler) : IScheduler {
    override fun flush() {
        parentScheduler.flush()
    }

    private val queue = QueueImpl<Pair<Long, () -> Unit>>()
    private var nextActionId = 0L

    override fun queue(action: () -> Unit) {
        val nxt = Sync.lock(queue) {
            val r = ++nextActionId
            queue.offer(r to action)
            r
        }
        parentScheduler.queue { pumpUntil(nxt) }
    }


    //what if lifetime terminates during pumping?
    fun pump() = pumpUntil(Long.MAX_VALUE)

    private fun pumpUntil(id: Long) {
        while (true) {
            Sync.lock(queue) {
                if (queue.isEmpty())
                    null
                else {
                    val (number, action) = queue.peek()!!
                    if (number <= id) {
                        queue.poll()
                        action
                    } else null
                }
            }?.invoke()?:return
        }
    }


    override val isActive: Boolean
        get() = parentScheduler.isActive

}

//todo multithreading
class ExtWire : IWire {

    override fun send(id: RdId, writer: (AbstractBuffer) -> Unit) {
        connectedWire.value?.send(id, writer)?: sendQ.add(id to createAbstractBuffer().also(writer).getArray())
    }

    override fun advise(lifetime: Lifetime, id: RdId, handler: (AbstractBuffer) -> Unit) {
        connectedWire.value?.advise(lifetime, id, handler) ?: subscriptionQ.add(Subscription(id, lifetime, null, handler))
    }

    override fun adviseOn(lifetime: Lifetime, id: RdId, scheduler: IScheduler, handler: (AbstractBuffer) -> Unit) {
        connectedWire.value?.adviseOn(lifetime, id, scheduler, handler) ?: subscriptionQ.add(Subscription(id, lifetime, scheduler, handler))
    }

    val connectedWire = Property<IWire?>(null)
    override val connected: IPropertyView<Boolean> = connectedWire.map { it != null }

    private val sendQ = ArrayList<Pair<RdId, ByteArray>>()
    private data class Subscription(val id: RdId, val lifetime: Lifetime, val scheduler: IScheduler?, val handler: (AbstractBuffer)->Unit)
    private val subscriptionQ = ArrayList<Subscription>()

    init {
        connectedWire.adviseNotNull(Lifetime.Eternal) { wire ->
            for ((id, payload) in sendQ) {
                wire.send(id) { it.writeByteArrayRaw(payload) }
            }
            sendQ.clear()

            for ((id, lifetime, scheduler, handler) in subscriptionQ) {
                if (scheduler == null)
                    wire.advise(lifetime, id, handler)
                else
                    wire.adviseOn(lifetime, id, scheduler, handler)
            }
            subscriptionQ.clear()
        }
    }

}