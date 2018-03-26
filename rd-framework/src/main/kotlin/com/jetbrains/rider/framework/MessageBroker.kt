package com.jetbrains.rider.framework

import com.jetbrains.rider.util.AtomicInteger
import com.jetbrains.rider.util.blockingPutUnique
import com.jetbrains.rider.util.getOrCreate
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.reactive.IScheduler
import com.jetbrains.rider.util.string.IPrintable
import com.jetbrains.rider.util.string.PrettyPrinter
import com.jetbrains.rider.util.string.condstr
import com.jetbrains.rider.util.trace

class MessageBroker(private val defaultScheduler: IScheduler) : IPrintable {

    companion object {
        val log = Protocol.sublogger("MQ")
    }

    private val lock = Any()

    private val subscriptions = hashMapOf<RdId, Subscription>()
    private val broker = hashMapOf<RdId, Mq>()

    private class Subscription(
        val broker: MessageBroker,
        val id: RdId,
        val scheduler: IScheduler,
        val handler: (AbstractBuffer) -> Unit
    ) {

        val inProcessCount = AtomicInteger(0)

        fun invoke(msg: AbstractBuffer) {
            inProcessCount.incrementAndGet()
            scheduler.queue {
                try {
                    if (synchronized(broker.lock) { broker.subscriptions.containsKey(id) })
                    //potential race condition here, but we know about it
                        handler(msg)
                    else
                        log.trace { "Handler for id $id disappeared" }
                } finally {
                    inProcessCount.decrementAndGet()
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
                        if (subscription.scheduler == defaultScheduler)
                            subscription.handler(message)
                        else
                            subscription.invoke(message)
                    } else {
                        log.trace { "No handler for id: $id" }
                    }

                    synchronized(lock) {
                        if (--broker[id]!!.defaultSchedulerMessages == 0) {
                            broker.remove(id)!!.customSchedulerMessages.forEach {
                                subscription?.apply {
                                    require (scheduler != defaultScheduler)
                                    subscription.invoke(message)
                                }
                            }
                        }
                    }
                }

            } else {

                if (s.scheduler == defaultScheduler || s.scheduler.outOfOrderExecution) {
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
    fun adviseOn(lifetime: Lifetime, scheduler: IScheduler, id: RdId, handler: (AbstractBuffer) -> Unit) {
        require(!id.isNull)

        //advise MUST happen under default scheduler, not custom
        defaultScheduler.assertThread()

        //if (lifetime.isTerminated) return
        subscriptions.blockingPutUnique(lifetime, lock, id, Subscription(this, id, scheduler, handler))
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
            printer.println("Subscribers: ${subscriptions.entries.sumBy { it.value.inProcessCount.get() }}")
            printer.println()
            printer.println("SubscriberId".padEnd(20) + "    " + "#MessagesInQueue")
            printer.println("".padStart(20, '-') + "----" + "".padStart(16, '-'))
            for ((key, value) in subscriptions) {
                printer.println("$key".padEnd(20) + " -> " + value.inProcessCount.get())
            }

        }
    }
}