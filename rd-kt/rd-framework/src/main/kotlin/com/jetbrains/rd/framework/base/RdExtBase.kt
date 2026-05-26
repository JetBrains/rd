package com.jetbrains.rd.framework.base

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.impl.ProtocolContexts
import com.jetbrains.rd.framework.impl.RdPropertyBase
import com.jetbrains.rd.util.*
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.isNotAlive
import com.jetbrains.rd.util.reactive.*
import com.jetbrains.rd.util.string.printToString
import com.jetbrains.rd.util.threading.asSequentialScheduler
import javax.management.openmbean.InvalidOpenTypeException

abstract class RdExtBase : RdReactiveBase() {
    enum class ExtState {
        Ready,
        ReceivedCounterpart,
        Disconnected
    }


    private val extWire = ExtWire()
    private var extProtocol: IProtocol? = null
    val connected = extWire.connected

    override val protocol: IProtocol? get() = extProtocol ?: super.protocol

    abstract val serializersOwner: ISerializersOwner
    open val serializationHash: Long = 0L

    var bindLifetime: Lifetime? = null
        private set

    override fun preInit(lifetime: Lifetime, parentProtocol: IProtocol) {
        bindLifetime = lifetime
        // will be called in init
    }

    override fun unbind() {
        customSchedulerWrapper = null
        bindLifetime = null
    }

    private var customSchedulerWrapper: CustomExtScheduler? = null

    protected open val extThreading: ExtThreadingKind get() = ExtThreadingKind.Default

    private val hasCustomScheduler: Boolean get() = extThreading == ExtThreadingKind.CustomScheduler
    private val allowCreationOnBackgroundThread: Boolean get() = extThreading != ExtThreadingKind.Default

    protected fun setCustomScheduler(scheduler: IScheduler) {
        require(hasCustomScheduler) { "Ext: ${this::javaClass.name} doesn't support custom schedulers" }

        val extScheduler = customSchedulerWrapper
        if (extScheduler == null) {
            if (bindLifetime?.isNotAlive != false)
                return
        }
        requireNotNull(extScheduler) { "Custom scheduler can only be set from ext listener only" }
        extScheduler.setScheduler(scheduler)
    }

    override fun init(lifetime: Lifetime, parentProtocol: IProtocol, ctx: SerializationCtx) {
        parentProtocol as Protocol
        Protocol.initializationLogger.traceMe { "binding" }

        val parentWire = parentProtocol.wire

        serializersOwner.register(parentProtocol.serializers)

        extWire.realWire = parentWire
        lifetime.bracketIfAlive({
            val scheduler = when (extThreading) {
                ExtThreadingKind.Default -> parentProtocol.scheduler
                ExtThreadingKind.CustomScheduler -> CustomExtScheduler()
                ExtThreadingKind.AllowBackgroundCreation -> parentProtocol.scheduler
            }

            val signal = createExtSignal(parentProtocol.identity)
            val proto = Protocol(parentProtocol.name, parentProtocol.serializers, parentProtocol.identity, scheduler, extWire, lifetime, parentProtocol, signal).also {
                it.outOfSyncModels.flowInto(lifetime, parentProtocol.outOfSyncModels) { model -> model }
            }

            extProtocol = proto
            Lifetime.using { activeLifetime ->
                if (scheduler is ExtSchedulerBase && extThreading != ExtThreadingKind.Default)
                    scheduler.setActiveCurrentThread(activeLifetime)

                AllowBindingCookie.allowBind {
                    super.preInit(lifetime, proto)
                    super.init(lifetime, proto, ctx)
                }

                val info = ExtCreationInfo(location, (parent as? RdBindableBase)?.rdid, serializationHash, this)

                if (scheduler is CustomExtScheduler) {
                    assert(customSchedulerWrapper == null)
                    customSchedulerWrapper = scheduler
                }

                Signal.nonPriorityAdviseSection {
                    parentProtocol.submitExtCreated(info)
                }
            }

            parentWire.advise(lifetime, this)

            parentWire.sendState(ExtState.Ready)
            Protocol.initializationLogger.traceMe { "created and bound :: ${printToString()}" }
        }, {
            extProtocol = null
            parentWire.sendState(ExtState.Disconnected)
        })
    }

    override fun assertBindingThread() = Unit

    override fun onWireReceived(proto: IProtocol, buffer: AbstractBuffer, ctx: SerializationCtx, dispatchHelper: IRdWireableDispatchHelper) {
        val remoteState = buffer.readEnum<ExtState>()
        logReceived.traceMe { "remote: $remoteState " }

        @Suppress("REDUNDANT_ELSE_IN_WHEN")
        when (remoteState) {
            ExtState.Ready -> {
                extWire.realWire.sendState(ExtState.ReceivedCounterpart)
                extWire.connected.value = true
            }

            ExtState.ReceivedCounterpart -> extWire.connected.set(true) //don't set anything if already set
            ExtState.Disconnected -> extWire.connected.set(false)

            else -> error("Unknown remote state: $remoteState")
        }

        val counterpartSerializationHash = buffer.readLong()
        val parentProtocol = super.protocol
        if (serializationHash != counterpartSerializationHash && parentProtocol != null) {
            //need to queue since outOfSyncModels is not synchronized
            parentProtocol.scheduler.queue { parentProtocol.outOfSyncModels.add(this) }
            error("serializationHash of ext '$location' doesn't match to counterpart: maybe you forgot to generate models?")
        }
    }

    private fun IWire.sendState(state: ExtState) {
        val proto = protocol ?: return

        proto.contexts.sendWithoutContexts {
            send(rdid) {
                logSend.traceMe { state }
                it.writeEnum(state)
                it.writeLong(serializationHash)
            }
        }
    }

    private inline fun Logger.traceMe(message: () -> Any) = this.trace { "ext `$location` ($rdid) :: ${message()}" }

    override fun initBindableFields(lifetime: Lifetime) {
        for ((_, child) in bindableChildren) {
            if (child is RdPropertyBase<*> && child.defaultValueChanged) {
                child.localChange {
                    child.bind()
                }
            } else {
                child?.bindPolymorphic()
            }
        }
    }

    enum class ExtThreadingKind {
        Default,
        CustomScheduler,

        @Deprecated("Creating on the background is allowed by default")
        AllowBackgroundCreation
    }
}

internal abstract class ExtSchedulerBase : IScheduler {
    private var activeThread = AtomicReference<Thread?>(null)

    internal fun setActiveCurrentThread(lifetime: Lifetime) {
        lifetime.bracketIfAliveEx({
            val thread = Thread.currentThread()
            val prevThread = activeThread.getAndSet(thread)
            assert(prevThread == null) { "prev thread must be null, but actual: $prevThread" }

            thread
        }, { thread ->
            val prevThread = activeThread.getAndSet(null)
            // parent lifetime can be terminated from background thread
            assert(prevThread != null) { "prev thread must not be null" }
        })
    }

    protected fun isActiveThread(): Boolean {
        return Thread.currentThread() == activeThread.get()
    }
}

internal class CustomExtScheduler : ExtSchedulerBase() {
    private val locker = Any()

    @Volatile
    private var queue: ArrayDeque<() -> Unit>? = ArrayDeque()

    @Volatile
    private lateinit var realScheduler: IScheduler

    override fun queue(action: () -> Unit) {
        val localQueue = queue
        if (localQueue == null) {
            realScheduler.queue(action)
            return
        }

        synchronized(locker) {
            val localQueue = queue
            if (localQueue != null) {
                localQueue.add(action)
            } else {
                realScheduler.queue(action)
            }
        }
    }

    override val isActive: Boolean
        get() = (queue == null && realScheduler.isActive) || isActiveThread()

    override val executionOrder: ExecutionOrder
        get() = ExecutionOrder.Sequential

    override fun flush() {
        throw InvalidOpenTypeException("Unsupported")
    }

    fun setScheduler(scheduler: IScheduler) {

        synchronized(locker) {
            require(!::realScheduler.isInitialized)

            val localQueue = queue
            require(localQueue != null) { "Scheduler already set" }

            while (true) {
                val acton = localQueue.removeFirstOrNull()
                if (acton != null) {
                    scheduler.queue(acton)
                    continue
                }

                realScheduler = scheduler.asSequentialScheduler()
                queue = null
                return
            }
        }
    }
}

//todo multithreading
class ExtWire : IWire {

    internal lateinit var realWire: IWire

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

                    val valueRestorers = context.map { it.first.updateValue(it.second) }
                    try {
                        realWire.send(id) { buffer -> buffer.writeByteArrayRaw(payload, count) }
                    } finally {
                        valueRestorers.forEach {
                            it.close()
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
                if (!contexts.isSendWithoutContexts) {
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

    override fun tryGetById(rdId: RdId): IRdWireable? {
        return realWire.tryGetById(rdId)
    }
}
