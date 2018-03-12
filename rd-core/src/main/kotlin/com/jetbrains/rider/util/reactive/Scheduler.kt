package com.jetbrains.rider.util.reactive

import java.util.concurrent.CountDownLatch

/**
 * Allows to queue the execution of actions on a different thread.
 */
interface IScheduler {
    /**
     * Queues the execution of the given [action].
     */
    fun queue(action: () -> Unit)
    val isActive: Boolean
    fun assertThread() {
        if (!isActive) {
            throw IllegalStateException("Illegal scheduler for current action, must be: $this, current thread: ${Thread.currentThread().name}")
        }
    }

    //Provides better performance but loose event consistency.
    val outOfOrderExecution : Boolean get() = false


    fun invokeOrQueue(action: () -> Unit) {
        if (isActive) action()
        else queue(action)
    }
}

fun IScheduler.flushScheduler() {
    val finishSignal = CountDownLatch(1)
    queue({ finishSignal.countDown() })

    finishSignal.await()
}
