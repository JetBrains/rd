package com.jetbrains.rd.framework

import com.jetbrains.rd.framework.base.AllowBindingCookie
import com.jetbrains.rd.framework.base.IRdWireable
import com.jetbrains.rd.framework.base.IRdWireableDispatchHelper
import com.jetbrains.rd.framework.impl.ProtocolContexts
import com.jetbrains.rd.util.Sync
import com.jetbrains.rd.util.blockingPutUnique
import com.jetbrains.rd.util.error
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.isAlive
import com.jetbrains.rd.util.lifetime.isNotAlive
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.string.IPrintable
import com.jetbrains.rd.util.string.PrettyPrinter
import com.jetbrains.rd.util.trace
import java.util.concurrent.LinkedBlockingQueue

class RdMessage (val id: RdId, val istream: AbstractBuffer, val tooBigMessage: String?)
class MessageBroker(queueMessages: Boolean = false) : IPrintable {

    companion object {
        val log = Protocol.sublogger("MQ")
    }

    private var unprocessedMessages: LinkedBlockingQueue<RdMessage>? = if (queueMessages) LinkedBlockingQueue() else null

    private val lock = Any()

    private val subscriptions = hashMapOf<RdId, Entry>()

    fun startDeliveringMessages() {
        while (true) {
            val queue: LinkedBlockingQueue<RdMessage>
            Sync.lock (lock) {
                queue = requireNotNull(unprocessedMessages) { "Already started delivering messages" }

                if (queue.size == 0)
                {
                    unprocessedMessages = null
                    return
                }

                unprocessedMessages = LinkedBlockingQueue()
            }

            for (rdMessage in queue) {
                dispatchImpl(rdMessage.id, rdMessage.istream, rdMessage.tooBigMessage)
            }
        }
    }

    fun tryGetById(rdId: RdId): IRdWireable? = tryGetEntryById(rdId)?.subscription

    private fun tryGetEntryById(rdId: RdId): Entry? {
        val value = Sync.lock(lock) { subscriptions[rdId] } ?: return null
        if (value.lifetime.isAlive)
            return value
        return null
    }

    //only on poller thread
    fun dispatch(id: RdId, buffer: AbstractBuffer, tooBigMessage: String?) {
        require(!id.isNull) { "id mustn't be null" }

        if (unprocessedMessages != null) {
            Sync.lock(lock) {
                val queue = unprocessedMessages ?: return@lock
                queue.add(RdMessage(id, buffer, tooBigMessage))
                return
            }
        }

        dispatchImpl(id, buffer, tooBigMessage)
    }

    private fun dispatchImpl(id: RdId, buffer: AbstractBuffer, tooBigMessage: String?) {
        val entry = tryGetEntryById(id)
        if (entry == null) {
            log.trace { "handler is not found for $id" }
            return
        }

        val protocol = entry.subscription.protocol
        if (protocol == null) {
            log.trace { "protocol is not found for $id" }
            return
        }

        try {
            AllowBindingCookie.allowBind {
                val messageContext = protocol.contexts.readContext(buffer)
                val helper = RdWireableDispatchHelper(entry.lifetime, id, protocol, messageContext)
                val subscription = entry.subscription
                if (tooBigMessage != null) {
                    log.error { tooBigMessage + " Location: " + subscription.location.toString() }
                }
                subscription.onWireReceived(buffer, helper)
            }
        } catch (e: Throwable) {
            log.error("Unexpected exception happened during processing a protocol event", e)
        }
    }

    private class RdWireableDispatchHelper(
        override val lifetime: Lifetime,
        override val rdId: RdId,
        private val protocol: IProtocol,
        private val messageContext: ProtocolContexts.MessageContext
    ) : IRdWireableDispatchHelper {

        override fun dispatch(scheduler: IScheduler?, action: () -> Unit) {
            doDispatch(lifetime, scheduler ?: protocol.scheduler, action)
        }

        private fun doDispatch(lifetime: Lifetime, scheduler: IScheduler, action: () -> Unit) {
            if (lifetime.isNotAlive) {
                log.trace { "Lifetime: $lifetime is not alive for $rdId" }
                return
            }

            scheduler.queue {
                if (lifetime.isNotAlive) {
                    log.trace { "Lifetime: $lifetime is not alive for $rdId" }
                    return@queue
                }

                messageContext.update {
                    action()
                }
            }
        }
    }

    fun adviseOn(lifetime: Lifetime, entity: IRdWireable) {
        if (entity.rdid.isNull) {
            if (lifetime.isNotAlive)
                return

            error("id is null for entity: $entity")
        }

        subscriptions.blockingPutUnique(lifetime, lock, entity.rdid, Entry(lifetime, entity))
    }

    override fun print(printer: PrettyPrinter) {
        Sync.lock(lock) {
            printer.println("MessageBroker Dump")
            printer.println("Subscribers:\n  ")
            printer.println(subscriptions.entries.joinToString("  "))
        }
    }

    private class Entry(val lifetime: Lifetime, val subscription: IRdWireable)
}
