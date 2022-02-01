package com.jetbrains.rd.util.threading

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.isAlive
import com.jetbrains.rd.util.reflection.threadLocal
import java.time.Duration

class SpinWait {
    companion object {
        @Deprecated("Use static/companion methods")
        val spin by threadLocal { SpinWait() }

        inline fun spinUntil(condition: () -> Boolean) {
            var spins = 0L
            while (!condition())
                if(spins++ < 100)
                    Thread.yield()
                else
                    Thread.sleep(spins / 100)
        }

        inline fun spinUntil(timeoutMs: Long, condition: () -> Boolean) = spinUntil(Lifetime.Eternal, timeoutMs, condition)
        inline fun spinUntil(lifetime: Lifetime, timeoutMs: Long, condition: () -> Boolean) = spinUntil(lifetime, Duration.ofMillis(timeoutMs), condition)

        //maybe CancellationException or timeout exception?
        inline fun spinUntil(lifetime: Lifetime, duration: Duration, condition: () -> Boolean) : Boolean {
            val start = System.nanoTime()
            var spins = 0L
            while (!condition()) {
                if (!lifetime.isAlive || System.nanoTime() - start > duration.toNanos())
                    return false
                if (spins++ < 100)
                    Thread.yield()
                else
                    Thread.sleep(spins / 100)
            }

            return true
        }
    }

    @Deprecated("Don't use: state needs to be maintained for proper implementation of backoff")
    fun spinOnce() {
        //todo need to rewrite it with exponential backoff, yield, sleep(0) and sleep(1)
        Thread.yield()
    }
}