package com.jetbrains.rider.util.threading

import com.jetbrains.rider.util.reactive.IScheduler
import com.jetbrains.rider.util.threadLocalWithInitial


object SynchronousScheduler : IScheduler {
    override fun flush() {

    }

    override fun queue(action: () -> Unit) {
        active.set(active.get() + 1)
        try {
            action()
        } finally {
            active.set(active.get() - 1)
        }
    }

    private val active =  threadLocalWithInitial { 0 }

    override val isActive: Boolean
        get() = active.get() > 0



}