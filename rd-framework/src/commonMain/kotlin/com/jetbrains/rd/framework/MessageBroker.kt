package com.jetbrains.rd.framework

import com.jetbrains.rd.framework.base.IRdReactive
import com.jetbrains.rd.util.assert
import com.jetbrains.rd.util.blockingPutUnique
import com.jetbrains.rd.util.getOrCreate
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.string.IPrintable
import com.jetbrains.rd.util.string.PrettyPrinter
import com.jetbrains.rd.util.string.condstr
import com.jetbrains.rd.util.trace

class MessageBroker(private val defaultScheduler: IScheduler) : IPrintable {

    companion object {
        val log = Protocol.sublogger("MQ")
    }

    private val lock = Any()

    private val subscriptions = hashMapOf<RdId, IRdReactive>()
    private val broker = hashMapOf<RdId, Mq>()

    private fun IRdReactive.invoke(msg: AbstractBuffer, sync: Boolean = false) {
        if (sync) { //todo think about scheduler.isActive()
            onWireReceived(msg) //error handling should process automatically
        } else {
            wireScheduler.queue {
                if (synchronized(lock) { subscriptions.containsKey(rdid) }) {
                    onWireReceived(msg)
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
    fun dispatch(id: RdId, message: AbstractBuffer) {
        require(!id.isNull) { "id mustn't be null" }

        synchronized(lock) {

            val s = subscriptions[id]
            if (s == null) {
                val currentIdBroker = broker.getOrCreate(id, { Mq() })

                currentIdBroker.defaultSchedulerMessages.add(message)
                defaultScheduler.queue {
                    val subscription = subscriptions[id] //no lock because can be changed only under default scheduler

                    val message = synchronized(lock) {
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

                    synchronized(lock) {
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
                    s.invoke(message)
                } else {
                    val mq = broker[id]
                    if (mq != null) {
                        require(mq.defaultSchedulerMessages.isNotEmpty())
                        mq.customSchedulerMessages.add(message)
                    } else {
                        s.invoke(message)
                    }
                }
            }

        }
    }

    //only on main thread
    fun adviseOn(lifetime: Lifetime, entity: IRdReactive) {
        require(!entity.rdid.isNull) {"id is null for entity: $entity"}

        //advise MUST happen under default scheduler, not custom
        defaultScheduler.assertThread(entity)

        //if (lifetime.isTerminated) return
        subscriptions.blockingPutUnique(lifetime, lock, entity.rdid, entity)

        if (entity.wireScheduler.outOfOrderExecution)
            lifetime.executeIfAlive {
                synchronized(lock) {
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
        synchronized(lock) {
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