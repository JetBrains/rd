package com.jetbrains.rd.framework

import com.jetbrains.rd.framework.base.IRdReactive
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
        var defaultSchedulerMessages: Int = 0
        val customSchedulerMessages = arrayListOf<AbstractBuffer>()
    }

    //only on poller thread
    fun dispatch(id: RdId, message: AbstractBuffer) {
        require(!id.isNull) { "id mustn't be null" }

        synchronized(lock) {

            val s = subscriptions[id]
            if (s == null) {
                broker.getOrCreate(id, { Mq() }).defaultSchedulerMessages++

                defaultScheduler.queue {
                    val subscription = subscriptions[id] //no lock because can be changed only under default scheduler

                    if (subscription != null) {
                        if (subscription.wireScheduler == defaultScheduler)
                            subscription.invoke(message, sync = true)
                        else
                            subscription.invoke(message)
                    } else {
                        log.trace { "No handler for id: $id" }
                    }

                    synchronized(lock) {
                        if (--broker[id]!!.defaultSchedulerMessages == 0) {
                            broker.remove(id)!!.customSchedulerMessages.forEach {
                                subscription?.apply {
                                    require (wireScheduler != defaultScheduler)
                                    subscription.invoke(it)
                                }
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
                        require(mq.defaultSchedulerMessages > 0)
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

        //advise MUST happen under default scheduler, not custom, unless it doesn't care
        if (!entity.wireScheduler.outOfOrderExecution)
            defaultScheduler.assertThread(entity)

        //if (lifetime.isTerminated) return
        subscriptions.blockingPutUnique(lifetime, lock, entity.rdid, entity)
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
                    printer.println("$key".padEnd(20) + " -> " + value.defaultSchedulerMessages +
                            (customSize > 0).condstr { " (+$customSize background messages)" })
                }
            }

            printer.println()
            printer.println("Subscribers:\n  ")
            printer.println(subscriptions.entries.joinToString("  "))
        }
    }
}