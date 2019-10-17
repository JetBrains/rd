package com.jetbrains.rd.framework.test.util

import com.jetbrains.rd.util.error
import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.info
import com.jetbrains.rd.util.reactive.IScheduler
import java.util.concurrent.LinkedBlockingDeque

/**
 * Scheduler executes queued actions sequentially when one call [flush] (on a thread where [flush] invoked).
 */
object SequentialPumpingScheduler : IScheduler {

    private val q = LinkedBlockingDeque<() -> Unit>()

    override fun queue(action: () -> Unit) {
        q.add(action)
    }


    private fun execute(action:() -> Unit) {
        val old = isActive
        isExecuting = true
        try {
            action()
        } catch (e: Throwable) {
            getLogger<SequentialPumpingScheduler>().error(e)
        } finally {
            isExecuting = old
        }
    }

    override val isActive: Boolean get() = true

    var isExecuting: Boolean = false
        private set


    override fun flush() {
        while (true) {
            val next = q.pollFirst() ?: return
            execute(next)
        }
    }

    val isEmpty: Boolean get() = q.size == 0
}
