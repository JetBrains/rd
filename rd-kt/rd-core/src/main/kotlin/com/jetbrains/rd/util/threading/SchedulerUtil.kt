package com.jetbrains.rd.util.threading

import com.jetbrains.rd.util.Logger
import com.jetbrains.rd.util.catch
import com.jetbrains.rd.util.error
import com.jetbrains.rd.util.reactive.ExecutionOrder
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.reflection.threadLocal
import com.jetbrains.rd.util.spinUntil
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asExecutor
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger

val Executor.asRdScheduler: IScheduler
    get() = asRdScheduler(ExecutionOrder.Unknown)

fun Executor.asRdScheduler(executionOrder: ExecutionOrder): IScheduler {
        val executor = this
        return executor as? IScheduler ?: object : IScheduler {

            private val tasksInQueue = AtomicInteger()
            private var active by threadLocal { 0 }

            override val isActive get() = active > 0

            override val executionOrder: ExecutionOrder
                get() = executionOrder

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

fun CoroutineDispatcher.asRdScheduler(dispatcherExecutionOrder: ExecutionOrder): IScheduler = (this as? IScheduler)
    ?: asExecutor().asRdScheduler(dispatcherExecutionOrder)

/**
 * Transforms the current scheduler into a sequential one.
 *
 * If the current scheduler already guarantees sequential execution, it is returned as is.
 * Otherwise, a new sequential scheduler is created that wraps the original scheduler.
 *
 * The returned sequential scheduler attempts to keep the original execution order
 * until concurrent execution is detected. When concurrency is detected (i.e. when a new task is
 * scheduled while a previous one hasn't finished yet), it stops preserving the original order and
 * queues tasks internally to ensure sequential execution.
 *
 * @return a scheduler that guarantees sequential execution.
 */
fun IScheduler.asSequentialScheduler(): IScheduler {
    if (executionOrder == ExecutionOrder.Sequential)
        return this

    return SequentialScheduler(this)
}

private class SequentialScheduler(private val realScheduler: IScheduler) : IScheduler {
    private val queue = ConcurrentLinkedQueue<() -> Unit>()
    private var thread: Thread? = null

    private var state = when (realScheduler.executionOrder) {
        ExecutionOrder.Sequential, ExecutionOrder.Unknown -> ActionKind.DelegateAsIs
        ExecutionOrder.OutOfOrder -> ActionKind.Repost
    }

    override val executionOrder: ExecutionOrder get() = ExecutionOrder.Sequential

    override val isActive: Boolean
        get() = thread == Thread.currentThread()

    override fun queue(action: () -> Unit) {
        queue.add(action)

        val delegateAsIs = synchronized(queue) {
            when (state) {
                ActionKind.Nothing -> return

                ActionKind.Repost -> {
                    state = ActionKind.Nothing
                    false
                }

                ActionKind.DelegateAsIs -> true
            }
        }

        if (delegateAsIs) {
            delegateAsIs()
        } else {
            repost()
        }
    }

    private fun delegateAsIs() {
        realScheduler.queue {

            synchronized(queue) {
                if (state != ActionKind.DelegateAsIs)
                    return@queue

                if (thread == null) {
                    thread = Thread.currentThread()
                } else {
                    // concurrent behavior detected
                    state = ActionKind.Nothing
                    return@queue
                }
            }

            val action = queue.poll()
            Logger.root.catch(action)

            synchronized(queue) {
                thread = null

                if (state == ActionKind.DelegateAsIs)
                    return@queue
            }

            repost()
        }
    }

    private fun repost() {
        assert(state == ActionKind.Nothing)

        realScheduler.queue {
            thread = Thread.currentThread()
            for (i in 0 until 16) {
                val action = queue.poll() ?: break

                Logger.root.catch(action)
            }
            thread = null

            synchronized(queue) {
                if (queue.peek() == null) {
                    state = ActionKind.Repost
                    return@queue
                }
            }

            repost()
        }
    }

    override fun flush() {
        require(!isActive) { "Can't flush this scheduler in a reentrant way: we are inside queued item's execution" }
        spinUntil { queue.isEmpty() }
    }

    private enum class ActionKind {
        Nothing,
        Repost,
        DelegateAsIs
    }
}
