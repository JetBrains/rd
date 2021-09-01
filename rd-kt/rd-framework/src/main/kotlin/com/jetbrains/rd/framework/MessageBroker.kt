package com.jetbrains.rd.framework

import com.jetbrains.rd.framework.base.IRdWireable
import com.jetbrains.rd.util.*
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.string.IPrintable
import com.jetbrains.rd.util.string.PrettyPrinter
import com.jetbrains.rd.util.string.condstr

class MessageBroker(private val defaultScheduler: IScheduler) : IPrintable {

    companion object {
        val log = Protocol.sublogger("MQ")
    }

    constructor(defaultScheduler: IScheduler, queueMessages: Boolean) : this(defaultScheduler) {
        isQueueingAllMessages = queueMessages
    }

    private val lock = Any()

    private val subscriptions = hashMapOf<RdId, IRdWireable>()
    private val broker = hashMapOf<RdId, Mq>()

    private var isQueueingAllMessages = false

    fun startDeliveringMessages() {
        Sync.lock(lock) {
            require(isQueueingAllMessages) { "Already delivering messages" }

            isQueueingAllMessages = false

            val entries = broker.entries.toList()
            broker.clear()

            entries.forEach { (id, mq) ->
                assert(mq.customSchedulerMessages.isEmpty()) { "Unexpected custom scheduler messages" }

                mq.defaultSchedulerMessages.forEach {
                    dispatch(id, it)
                }
            }
        }
    }

    private fun IRdWireable.invoke(msg: AbstractBuffer, sync: Boolean = false) {
        if (sync) { //todo think about scheduler.isActive()
            protocol.contexts.readMessageContextAndInvoke(msg) {
                onWireReceived(msg) //error handling should process automatically
            }
        } else {
            wireScheduler.queue {
                if (isBound && Sync.lock(lock) { subscriptions.containsKey(rdid) }) {
                    protocol.contexts.readMessageContextAndInvoke(msg) {
                        onWireReceived(msg)
                    }
                } else {
                    log.trace { "Handler for $this dissapeared" }
                }
            }
        }
    }




    private class Mq {
        var defaultSchedulerMessages = arrayListOf<AbstractBuffer>()
        val customSchedulerMessages = arrayListOf<AbstractBuffer>()
    }

    //only on poller thread
    fun dispatch(id: RdId, buffer: AbstractBuffer) {
        require(!id.isNull) { "id mustn't be null" }

        Sync.lock(lock) {

            val s = subscriptions[id]
            if (s == null || isQueueingAllMessages) {
                val currentIdBroker = broker.getOrCreate(id) { Mq() }

                currentIdBroker.defaultSchedulerMessages.add(buffer)

                if(isQueueingAllMessages) return

                defaultScheduler.queue {
                    val subscription = subscriptions[id] //no lock because can be changed only under default scheduler

                    val message = Sync.lock(lock) {
                        if (currentIdBroker.defaultSchedulerMessages.isNotEmpty())
                            currentIdBroker.defaultSchedulerMessages.removeAt(0)
                        else // messages could have been consumed by adviseOn
                            null
                    }

                    // no lock for processing broker contents as all additions happen before subscriptions[id] write,
                    // and it happens on default scheduler (asserted) before this handler is executed
                    // also, if adviseOn executes messages, it clears this list, also on the same thread as this
                    if (subscription == null) {
                        log.trace { "No handler for id: $id" }
                    } else if(message != null) {
                        subscription.invoke(message, sync = subscription.wireScheduler == defaultScheduler)
                    }

                    Sync.lock(lock) {
                        if (currentIdBroker.defaultSchedulerMessages.isEmpty())
                            broker.remove(id)?.customSchedulerMessages?.forEach { // use result of remove for reentrancy cases
                                subscription?.apply {
                                    require(wireScheduler != defaultScheduler)
                                    subscription.invoke(it)
                                }
                            }
                    }
                }
            } else {

                if (s.wireScheduler == defaultScheduler || s.wireScheduler.outOfOrderExecution) {
                    s.invoke(buffer)
                } else {
                    val mq = broker[id]
                    if (mq != null) {
                        mq.customSchedulerMessages.add(buffer)
                    } else {
                        s.invoke(buffer)
                    }
                }
            }

        }
    }

    //only on main thread
    fun adviseOn(lifetime: Lifetime, entity: IRdWireable) {
        require(!entity.rdid.isNull) {"id is null for entity: $entity"}

        //advise MUST happen under default scheduler, not custom
        //todo commented because of wiredRdTask
//        defaultScheduler.assertThread(entity)

        //if (lifetime.isTerminated) return
        subscriptions.blockingPutUnique(lifetime, lock, entity.rdid, entity)

        if (entity.wireScheduler.outOfOrderExecution)
            lifetime.executeIfAlive {
                Sync.lock(lock) {
                    broker.remove(entity.rdid)?.let { mq ->
                        mq.defaultSchedulerMessages.forEach { msg ->
                            entity.invoke(msg)
                        }
                        mq.defaultSchedulerMessages.clear() // clear it here because it is captured by default scheduler queueing
                        assert(mq.customSchedulerMessages.isEmpty()) { "Custom scheduler messages for an entity with outOfOrder scheduler $entity" }
                    }
                }
                Unit
            }
    }

    override fun print(printer: PrettyPrinter) {
        Sync.lock(lock) {
            printer.println("MessageBroker Dump")

            printer.println()
            printer.println("Messages to unsubscribed: ${broker.size}")
            if (broker.size > 0) {
                printer.println()
                printer.println("Id".padEnd(20) + "    " + "#Messages")
                printer.println("".padStart(20, '-') + "----" + "".padStart(9, '-'))
                for ((key, value) in broker) {
                    val customSize = value.customSchedulerMessages.size
                    printer.println("$key".padEnd(20) + " -> " + value.defaultSchedulerMessages.size +
                            (customSize > 0).condstr { " (+$customSize background messages)" })
                }
            }

            printer.println()
            printer.println("Subscribers:\n  ")
            printer.println(subscriptions.entries.joinToString("  "))
        }
    }
}