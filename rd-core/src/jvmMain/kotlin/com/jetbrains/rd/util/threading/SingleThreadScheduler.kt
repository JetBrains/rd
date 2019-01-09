package com.jetbrains.rd.util.threading

import com.jetbrains.rd.util.error
import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.plusAssign
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.reflection.threadLocal
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

abstract class SingleThreadSchedulerBase(val name: String) : IScheduler {
    abstract fun onException(ex: Throwable)

    private fun createThreadFactory() = ThreadFactory { r ->
        Thread(r, name)
                .apply { isDaemon = true }
                .apply { priority = Thread.NORM_PRIORITY }

    }

    val executor: ThreadPoolExecutor = object : ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, LinkedBlockingQueue<Runnable>(), createThreadFactory()) {
        override fun afterExecute(r: Runnable?, t: Throwable?) {
            super.afterExecute(r, t)

            if (t == null) return
            onException(t)
        }
    }
    val tasksInQueue = AtomicInteger(0)

    override fun queue(action: () -> Unit) {
        tasksInQueue.incrementAndGet()
        executor.execute {
            active++
            try {
                action()
            } finally {
                active --
                tasksInQueue.decrementAndGet()
            }
        }
    }

    override fun toString(): String = name

    private var active : Int by threadLocal {0}

    override val isActive: Boolean get() = active > 0

    override fun flush() {
        require(!isActive) {"Can't flush this scheduler in a reentrant way: we are inside queued item's execution"}

        SpinWait.spinUntil { tasksInQueue.get() == 0 }
    }
}

class SingleThreadScheduler(val lifetime: Lifetime, name : String) : SingleThreadSchedulerBase(name) {
    private val log = getLogger<SingleThreadScheduler>()

    init {
        lifetime += {
            try {
                executor.shutdownNow()

                if (!executor.awaitTermination(5, TimeUnit.SECONDS))
                    log.error { "Failed to terminate $name." }
            } catch(e: Throwable) {
                log.error(e)
            }
        }
    }

    override fun onException(ex: Throwable) {
        log.error(ex)
    }
}

class TestSingleThreadScheduler(name : String) : SingleThreadSchedulerBase(name) {
    private var thrownExceptions = mutableListOf<Throwable>()

    override fun onException(ex: Throwable) {
        thrownExceptions.add(ex)
    }

    fun assertNoExceptions() {
        flush()
        val exceptions = ArrayList(thrownExceptions)
        thrownExceptions.clear()
        CompoundThrowable.throwIfNotEmpty(exceptions)
    }
}

