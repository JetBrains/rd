package com.jetbrains.rd.util.threading

import com.jetbrains.rd.util.Logger
import com.jetbrains.rd.util.error
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.reflection.threadLocal
import com.jetbrains.rd.util.spinUntil
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asExecutor
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger

val Executor.asRdScheduler: IScheduler
    get() {
        val executor = this
        return executor as? IScheduler ?: object : IScheduler {

            private val tasksInQueue = AtomicInteger()
            private var active by threadLocal { 0 }

            override val isActive get() = active > 0

            override fun queue(action: () -> Unit) {
                tasksInQueue.incrementAndGet()
                executor.execute {
                    active++
                    try {
                        action()
                    } catch (e: Throwable) {
                        Logger.root.error(e)
                    } finally {
                        active--
                        tasksInQueue.decrementAndGet()
                    }
                }
            }

            override fun flush() {
                require(!isActive) { "Can't flush this scheduler in a reentrant way: we are inside queued item's execution" }
                spinUntil { tasksInQueue.get() == 0 }
            }
        }
    }

val CoroutineDispatcher.asRdScheduler: IScheduler
    get() = (this as? IScheduler) ?: asExecutor().asRdScheduler
