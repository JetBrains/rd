package com.jetbrains.rd.framework.base

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.impl.HeavySingleContextHandler
import com.jetbrains.rd.framework.impl.ProtocolContexts
import com.jetbrains.rd.framework.impl.RdPropertyBase
import com.jetbrains.rd.util.Logger
import com.jetbrains.rd.util.Queue
import com.jetbrains.rd.util.Sync
import com.jetbrains.rd.util.collections.QueueImpl
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.reactive.Property
import com.jetbrains.rd.util.reactive.flowInto
import com.jetbrains.rd.util.reactive.whenTrue
import com.jetbrains.rd.util.string.printToString
import com.jetbrains.rd.util.threading.SynchronousScheduler
import com.jetbrains.rd.util.trace

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
    override val wireScheduler: IScheduler get() = SynchronousScheduler

    override val protocol: IProtocol get() = extProtocol ?: super.protocol //nb

    abstract val serializersOwner: ISerializersOwner
    open val serializationHash: Long = 0L


    override fun init(lifetime: Lifetime) {
        Protocol.initializationLogger.traceMe { "binding" }

        val parentProtocol = super.protocol
        val parentWire = parentProtocol.wire

        serializersOwner.register(parentProtocol.serializers)

//        val sc = ExtScheduler(parentProtocol.scheduler)
        val sc = parentProtocol.scheduler
        extWire.realWire = parentWire
        lifetime.bracket(
            {
//                extScheduler = sc
                extProtocol = Protocol(parentProtocol.name, parentProtocol.serializers, parentProtocol.identity, sc, extWire, lifetime, serializationContext, parentProtocol.contexts).also {
                    it.outOfSyncModels.flowInto(lifetime, super.protocol.outOfSyncModels) { model -> model }
                }
            },
            {
                extProtocol = null
//                extScheduler = null
            }
        )

        parentWire.advise(lifetime, this)

        //it's critical to advise before 'Ready' is sent because we advise on SynchronousScheduler

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

    override fun onWireReceived(buffer: AbstractBuffer) {
        val remoteState = buffer.readEnum<ExtState>()
        logReceived.traceMe { "remote: $remoteState " }

        @Suppress("REDUNDANT_ELSE_IN_WHEN")
        when (remoteState) {
            RdExtBase.ExtState.Ready -> {
                extWire.realWire.sendState(ExtState.ReceivedCounterpart)
                extWire.connected.value = true
            }
            RdExtBase.ExtState.ReceivedCounterpart -> extWire.connected.set(true) //don't set anything if already set
            RdExtBase.ExtState.Disconnected -> extWire.connected.set(false)

            else -> error("Unknown remote state: $remoteState")
        }

        val counterpartSerializationHash = buffer.readLong()
        if (serializationHash != counterpartSerializationHash) {
            //need to queue since outOfSyncModels is not synchronized
            super.protocol.scheduler.queue { super.protocol.outOfSyncModels.add(this) }
            error("serializationHash of ext '$location' doesn't match to counterpart: maybe you forgot to generate models?")
        }
    }

    private fun IWire.sendState(state: ExtState) = protocol.contexts.sendWithoutContexts {
        send(rdid) {
            logSend.traceMe { state }
            it.writeEnum(state)
            it.writeLong(serializationHash)
        }
    }
    private inline fun Logger.traceMe (message:() -> Any) = this.trace { "ext `$location` ($rdid) :: ${message()}" }

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

    internal lateinit var realWire : IWire

    override fun advise(lifetime: Lifetime, entity: IRdWireable) = realWire.advise(lifetime, entity)

    override val contexts: ProtocolContexts
        get() = realWire.contexts

    override fun setupContexts(newContexts: ProtocolContexts) {
        require(newContexts === realWire.contexts) { "Can't replace ProtocolContexts on ExtWire" }
    }

    @Suppress("ArrayInDataClass")
    data class QueueItem(val id: RdId, val msgSize: Int, val payoad: ByteArray, val context: List<Pair<RdContext<Any>, Any?>>)
    override val connected: Property<Boolean> = Property(false)
    override val heartbeatAlive
        get() = realWire.heartbeatAlive

    override var heartbeatIntervalMs: Long
        get() = realWire.heartbeatIntervalMs
        set(duration) {
            realWire.heartbeatIntervalMs = duration
        }


    private val sendQ = Queue<QueueItem>()

    init {
        connected.whenTrue(Lifetime.Eternal) { _ ->
            Sync.lock(sendQ) {
                while (true) {
                    val (id, count, payload, context) = sendQ.poll() ?: return@lock

                    if (context.isEmpty()) {
                        realWire.contexts.sendWithoutContexts {
                            realWire.send(id) { buffer -> buffer.writeByteArrayRaw(payload, count) }
                        }
                        continue
                    }

                    val prevValues = ArrayList<Any?>(context.size)
                    context.forEach { (ctx, value) ->
                        prevValues.add(ctx.value)
                        ctx.value = value
                    }
                    try {
                        realWire.send(id) { buffer -> buffer.writeByteArrayRaw(payload, count) }
                    } finally {
                        context.forEachIndexed { idx, (ctx, _) ->
                            ctx.value = prevValues[idx]
                        }
                    }

                }
            }
        }
    }


    override fun send(id: RdId, writer: (AbstractBuffer) -> Unit) {
        Sync.lock(sendQ) {
            if (!sendQ.isEmpty() || !connected.value) {
                val buffer = createAbstractBuffer()
                writer(buffer)
                @Suppress("UNCHECKED_CAST")
                sendQ.offer(QueueItem(id, buffer.position, buffer.getArray(),
                    if (contexts.isSendWithoutContexts)
                        emptyList()
                    else
                        contexts.registeredContexts.map { (it as RdContext<Any>) to it.value })
                )
                if(!contexts.isSendWithoutContexts) {
                    // trigger value set addition here to replicate normal wire behavior
                    contexts.registeredContexts.forEach { context ->
                        contexts.getContextHandler(context).registerValueInValueSet()
                    }
                }
                return
            }

        }

        realWire.send(id, writer)
    }

}
