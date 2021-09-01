package com.jetbrains.rd.util.test.cases

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.*
import com.jetbrains.rd.util.threading.SynchronousScheduler
import com.jetbrains.rd.util.throttleLast
import org.junit.jupiter.api.Test
import java.time.Duration

class SignalThrottleTest {
    @Test
    fun testThrottle() {
        val orig = Signal<Int>()
        val throttled = orig.throttleLast(Duration.ofMillis(200), SynchronousScheduler)

        var value = 0
        Lifetime.using { lt ->
            throttled.advise(lt) {
                println(it)
            }

            repeat(40) {
                orig.fire(value++)
                Thread.sleep(20)
            }
        }
    }
}
