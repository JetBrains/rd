package com.jetbrains.rd.benchmarks

import com.jetbrains.rider.util.reactive.IScheduler

class BenchmarkScheduler : IScheduler {
    override fun queue(action: () -> Unit) = action()

    override val isActive: Boolean = true
    override fun flush() {
    }
}