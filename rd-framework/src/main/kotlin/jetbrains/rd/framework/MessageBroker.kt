package com.jetbrains.rider.framework

import com.jetbrains.rider.util.*
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.reactive.IScheduler
import org.apache.commons.logging.LogFactory
import java.io.InputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.atomic.AtomicInteger

class MessageBroker(private val defaultScheduler: IScheduler) {

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
            val handler: (InputStream) -> Unit
            ) {

        val inProcessCount = AtomicInteger()

        fun invoke(msg: InputStream) {
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
        var defaultSchedulerMessages : Int = 0
        val customSchedulerMessages = arrayListOf<InputStream>()
    }

    //only on poller thread
    fun dispatch(id: RdId, message: InputStream) {
        require(!id.isNull)

        synchronized(lock) {

            val s = subscriptions[id]
            if (s == null) {
                broker.getOrCreate(id, {Mq()}).defaultSchedulerMessages++

                defaultScheduler.queue {
                    val subscription = subscriptions[id] //no lock because can be changed only under default scheduler

                    if (subscription != null) {
                        if (subscription.scheduler == defaultScheduler)
                            subscription.handler(message)
                        else
                            subscription.invoke(message)
                    } else {
                        log.trace {"No handler for id: $id"}
                    }

                    synchronized(lock) {
                        if (-- broker[id]!!.defaultSchedulerMessages == 0) {
                            broker.remove(id)!!.customSchedulerMessages.forEach {
                                subscription?.apply {
                                    assert (scheduler != defaultScheduler)
                                    subscription.invoke(message)
                                }
                            }
                        }
                    }
                }


            } else {

                if (s.scheduler == defaultScheduler || s.scheduler.outOfOrderExecution) {
                    s.invoke(message)
                }

                else {
                    val mq = broker[id]
                     if (mq != null) {
                       assert (mq.defaultSchedulerMessages > 0)
                       mq.customSchedulerMessages.add(message)
                     } else {
                         s.invoke(message)
                     }
                }
            }

        }
    }

    //only on main thread
    fun adviseOn(lifetime: Lifetime, scheduler: IScheduler, id: RdId, handler: (InputStream) -> Unit) {
        require(!id.isNull)

        //advise MUST happen under default scheduler, not custom
        defaultScheduler.assertThread()

        //if (lifetime.isTerminated) return
        subscriptions.blockingPutUnique(lifetime, lock, id, Subscription(this, id, scheduler, handler))
    }


    //Diagnostics
    fun dump(out: PrintWriter) {
        synchronized(lock) {
            out.println("MessageBroker Dump")

            out.println()
            out.println("Messages to unsubscribed: ${broker.size}")
            if (broker.size > 0) {
                out.println()
                out.println("Id".padEnd(20) + "    " + "#Messages")
                out.println("".padStart(20, '-') + "----" + "".padStart(9, '-'))
                for ((key, value) in broker) {
                    val customSize = value.customSchedulerMessages.size
                    out.println("$key".padEnd(20) + " -> " + value.defaultSchedulerMessages +
                            (customSize > 0).condstr { " (+$customSize background messages)" })
                }
            }

            out.println()
            out.println("Subscribers: ${subscriptions.entries.sumBy { it.value.inProcessCount.get() }}")
            out.println()
            out.println("SubscriberId".padEnd(20) + "    " + "#MessagesInQueue")
            out.println("".padStart(20, '-') + "----" + "".padStart(16, '-'))
            for ((key, value) in subscriptions) {
                out.println("$key".padEnd(20) + " -> " + value.inProcessCount.get())
            }

        }
    }

    fun dumpToString() = StringWriter().apply { dump(PrintWriter(this, true)) }.toString()

}