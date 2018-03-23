package com.jetbrains.rider.util

import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.lifetime.plusAssign
import com.jetbrains.rider.util.reactive.IScheduler
import com.jetbrains.rider.util.reactive.ISource
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Duration
import java.util.*
import kotlin.concurrent.schedule

private val timer by lazy { Timer("rd throttler", true) }

fun <T : Any> ISource<T>.throttleLast(timeout: Duration, scheduler: IScheduler) = object : ISource<T> {

    override fun advise(lifetime: Lifetime, handler: (T) -> Unit) {
        var currentTask: TimerTask? = null
        val lastValue = AtomicReference<T?>(null)

        if (lifetime.isTerminated) return
        lifetime += { currentTask?.cancel() }

        this@throttleLast.advise(lifetime) { v ->
            if (lastValue.getAndSet(v) == null) {
                currentTask = timer.schedule(timeout.toMillis()) {
                    val toSchedule = lastValue.getAndSet(null)?: return@schedule
                    scheduler.invokeOrQueue {
                        if (!lifetime.isTerminated)
                            handler(toSchedule)
                    }
                }
            }
        }

    }
}

fun Throwable.getThrowableText(): String {
    val stringWriter = StringWriter()
    val writer = PrintWriter(stringWriter)
    printStackTrace(writer)
    return stringWriter.buffer.toString()
}