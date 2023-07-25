package com.jetbrains.rd.util.reactive

import com.jetbrains.rd.util.Logger
import com.jetbrains.rd.util.currentThreadName
import com.jetbrains.rd.util.error

/**
 * Allows to queue the execution of actions on a different thread.
 */
interface IScheduler {
    /**
     * Queues the execution of the given [action].
     */
    fun queue(action: () -> Unit)
    val isActive: Boolean
    fun assertThread(debugInfo: Any? = null) {
        if (!isActive) {
            Logger.root.error {
                "Illegal scheduler for current action, must be: $this, current thread: ${currentThreadName()}" +
                    (debugInfo?.let { ", debug info: $it" } ?:"")
            }
        }
    }

    val executionOrder: ExecutionOrder

    fun invokeOrQueue(action: () -> Unit) {
        if (isActive) action()
        else queue(action)
    }

    fun flush()
}

interface ISchedulerWithBackground : IScheduler {
    /**
     * An associated scheduler which executes all tasks outside of the main thread
     */
    val backgroundScheduler: IScheduler
}

//Provides better performance but loose event consistency.
val IScheduler.outOfOrderExecution : Boolean get() = executionOrder == ExecutionOrder.OutOfOrder

/**
 * Represents the execution order guarantee of a scheduler.
 */
enum class ExecutionOrder {
    /**
     * The scheduler guarantees a sequential execution order.
     * Tasks are executed in the order they were received.
     */
    Sequential,

    /**
     * The scheduler does not guarantee a sequential execution order.
     * Tasks may be executed concurrently or in a different order than received.
     */
    OutOfOrder,

    /**
     * The execution order of the scheduler is unknown.
     * This is typically used when the scheduler is a wrapper around another service or dispatcher
     * where the execution order cannot be directly determined.
     * It is important to note that 'Unknown' should not be treated as 'OutOfOrder'. It may represent
     * a sequential scheduler and any optimization assuming an 'OutOfOrder' execution may potentially
     * disrupt the actual execution order.
     */
    Unknown
}

