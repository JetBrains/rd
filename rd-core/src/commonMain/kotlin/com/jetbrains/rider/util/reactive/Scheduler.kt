package com.jetbrains.rider.util.reactive

import com.jetbrains.rider.util.Logger
import com.jetbrains.rider.util.currentThreadName
import com.jetbrains.rider.util.error
import com.jetbrains.rider.util.reflection.threadLocal

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

    //Provides better performance but loose event consistency.
    val outOfOrderExecution : Boolean get() = false


    fun invokeOrQueue(action: () -> Unit) {
        if (isActive) action()
        else queue(action)
    }

    fun flush()
}