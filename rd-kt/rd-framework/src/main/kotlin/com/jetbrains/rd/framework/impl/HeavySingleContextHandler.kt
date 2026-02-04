package com.jetbrains.rd.framework.impl

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.IRdBindable
import com.jetbrains.rd.framework.base.IRdWireableDispatchHelper
import com.jetbrains.rd.framework.base.ISingleContextHandler
import com.jetbrains.rd.framework.base.RdReactiveBase
import com.jetbrains.rd.util.*
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.*
import com.jetbrains.rd.util.string.PrettyPrinter
import com.jetbrains.rd.util.string.print
import com.jetbrains.rd.util.string.printToString

internal class HeavySingleContextHandler<T : Any>(override val context: RdContext<T>, private val contexts: ProtocolContexts) : RdReactiveBase(), ISingleContextHandler<T> {
    private val protocolValueSet = ConcurrentRdSet(context.serializer, contexts)
    @Suppress("UNCHECKED_CAST")
    private val internRoot = InternRoot(context.serializer)

    override fun deepClone(): IRdBindable {
        error("This may not be cloned")
    }

    private fun sendWithoutContexts(block: () -> Unit) {
        contexts.sendWithoutContexts(block)
    }

    val valueSet : IAppendOnlyViewableConcurrentSet<T>
        get() = protocolValueSet

    override fun preInit(lifetime: Lifetime, proto: IProtocol) {
        super.preInit(lifetime, proto)

        protocolValueSet.rdid = proto.identity.mix(rdid, "ValueSet")
        protocolValueSet.preBind(lifetime, this, "ValueSet")

        internRoot.rdid = proto.identity.mix(rdid, "InternRoot")
        internRoot.preBind(lifetime, this, "InternRoot")
    }

    override fun init(lifetime: Lifetime, proto: IProtocol, ctx: SerializationCtx) {
        super.init(lifetime, proto, ctx)

        assert(contexts.isSendWithoutContexts) { "Must bind context handler without sending contexts to prevent reentrancy" }

        protocolValueSet.bind()
        internRoot.bind()

        protocolValueSet.view(lifetime) { valueLifetime, value ->
            handleValueAddedToProtocolSet(valueLifetime, value)
        }
    }

    private fun handleValueAddedToProtocolSet(lifetime: Lifetime, value: T) {
        sendWithoutContexts {
            lifetime.bracketIfAlive({
                internRoot.intern(value)
            }, {
                internRoot.remove(value)
            })
        }
    }

    override fun onWireReceived(proto: IProtocol, buffer: AbstractBuffer, ctx: SerializationCtx, dispatchHelper: IRdWireableDispatchHelper) {
        error("SingleKeyContextHandler does not receive own messages")
    }

    @Suppress("UNCHECKED_CAST")
    override fun writeValue(ctx: SerializationCtx, buffer: AbstractBuffer) {
        assert(!contexts.isSendWithoutContexts) { "Trying to write context with a context-related message, key ${context.key}"}
        val value = context.value
        if(value == null) {
            buffer.writeInternId(InternId.invalid)
            buffer.writeBoolean(false)
        } else {
            sendWithoutContexts {
                addCurrentValueToValueSet(value)

                val internedId = internRoot.intern(value)
                buffer.writeInternId(internedId)
                if (!internedId.isValid) {
                    buffer.writeBoolean(true)
                    context.serializer.write(ctx, buffer, value)
                }
            }
        }
    }

    override fun registerValueInValueSet() {
        val value = context.value ?: return

        sendWithoutContexts {
            addCurrentValueToValueSet(value)
        }
    }

    private fun addCurrentValueToValueSet(value: T) {
        protocolValueSet.add(value)
    }

    override fun readValue(ctx: SerializationCtx, buffer: AbstractBuffer): T? {
        val id = buffer.readInternId()
        return if (!id.isValid) {
            val hasValue = buffer.readBoolean()
            if(hasValue)
                context.serializer.read(ctx, buffer)
            else
                null
        } else
            internRoot.tryUnIntern(id)
    }

    override var async: Boolean
        get() = true
        set(value) = error("SingleKeyContextHandler is always async")

    internal class ConcurrentRdSet<T>(
        private val valueSerializer: ISerializer<T>,
        private val protocolContexts: ProtocolContexts,
    ) : RdReactiveBase(), IAppendOnlyViewableConcurrentSet<T> {


        override var rdid: RdId = RdId.Null

        private val set = ConcurrentViewableSet<T>()
        private val isThreadLocalChange = threadLocalWithInitial { false }

        override val size: Int
            get() = set.size

        init {
            async = true
        }

        override fun preInit(lifetime: Lifetime, proto: IProtocol) {
            super.preInit(lifetime, proto)
            proto.wire.advise(lifetime, this)
        }

        override fun init(lifetime: Lifetime, proto: IProtocol, ctx: SerializationCtx) {
            super.init(lifetime, proto, ctx)
            view(lifetime) { _, value ->
                if (!isThreadLocalChange.get()) return@view

                proto.wire.send(rdid) { writer ->

                    val kind = AddRemove.Add
                    writer.writeEnum(kind)
                    valueSerializer.write(ctx, writer, value)

                    logSend.trace { "set `$location` ($rdid) :: $kind :: ${value.printToString()} "}
                }
            }
        }

        override fun add(value: T): Boolean {
            assert(!isThreadLocalChange.get())

            isThreadLocalChange.set(true)
            try {
                return protocolContexts.sendWithoutContexts {
                    set.add(value)
                }
            } finally {
                isThreadLocalChange.set(false)
            }
        }

        override fun contains(value: T): Boolean {
            return set.contains(value)
        }

        override fun view(lifetime: Lifetime, action: (Lifetime, T) -> Unit) {
            set.view(lifetime, action)
        }

        override fun onWireReceived(proto: IProtocol, buffer: AbstractBuffer, ctx: SerializationCtx, dispatchHelper: IRdWireableDispatchHelper) {
            val kind = buffer.readEnum<AddRemove>()
            val v = valueSerializer.read(ctx, buffer)

            when (kind) {
                AddRemove.Add -> set.add(v)
                AddRemove.Remove -> set.remove(v)
            }
        }

        override fun iterator() = set.iterator()

        override fun print(printer: PrettyPrinter) {
            super.print(printer)
            printer.print(" [")
            if (size > 0) printer.println()

            printer.indent {
                forEach {
                    it.print(printer)
                    printer.println()
                }
            }
            printer.print("]")
        }
    }
}