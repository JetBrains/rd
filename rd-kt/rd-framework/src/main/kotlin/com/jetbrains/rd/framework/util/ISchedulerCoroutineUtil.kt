package com.jetbrains.rd.framework.util

import com.jetbrains.rd.util.reactive.IScheduler
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.coroutines.CoroutineContext

private class SchedulerCoroutineDispatcher(private val scheduler: IScheduler, private val allowInlining: Boolean) : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) = scheduler.queue { block.run() }
    override fun isDispatchNeeded(context: CoroutineContext) = !allowInlining || !scheduler.isActive
}

val IScheduler.asCoroutineDispatcher get() = (this as? CoroutineDispatcher) ?: asCoroutineDispatcher(false)
fun IScheduler.asCoroutineDispatcher(allowInlining: Boolean): CoroutineDispatcher = SchedulerCoroutineDispatcher(this, allowInlining)
