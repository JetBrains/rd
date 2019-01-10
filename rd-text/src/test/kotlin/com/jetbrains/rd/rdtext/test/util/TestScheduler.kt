package com.jetbrains.rd.rdtext.test.util

import com.jetbrains.rd.util.reactive.IScheduler

object TestScheduler : IScheduler {
    override fun flush() {}
    override fun queue(action: () -> Unit) = action()
    override val isActive: Boolean get() = true
}