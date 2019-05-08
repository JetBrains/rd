package com.jetbrains.rd.util.threading

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reflection.threadLocal
import java.time.Duration

class SpinWait {
    companion object {
        val spin by threadLocal { SpinWait() }

        inline fun spinUntil(condition: () -> Boolean) {
            val s = spin
            while (!condition())
                s.spinOnce()
        }

        inline fun spinUntil(timeoutMs: Long, condition: () -> Boolean) = spinUntil(Lifetime.Eternal, timeoutMs, condition)
        inline fun spinUntil(lifetime: Lifetime, timeoutMs: Long, condition: () -> Boolean) = spinUntil(lifetime, Duration.ofMillis(timeoutMs), condition)

        //maybe CancellationException or timeout exception?
        inline fun spinUntil(lifetime: Lifetime, duration: Duration, condition: () -> Boolean) : Boolean {
            val s = spin
            val start = System.nanoTime()
            while (!condition()) {
                if (lifetime.isTerminated || System.nanoTime() - start > duration.toNanos())
                    return false
                s.spinOnce()
            }

            return true
        }
    }

    fun spinOnce() {
        //do a little spin
        Thread.yield()
    }
}