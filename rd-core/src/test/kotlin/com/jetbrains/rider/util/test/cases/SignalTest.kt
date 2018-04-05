package com.jetbrains.rider.util.test.cases

import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.lifetime.plusAssign
import com.jetbrains.rider.util.reactive.ISignal
import com.jetbrains.rider.util.reactive.Signal
import com.jetbrains.rider.util.reactive.adviseEternal
import com.jetbrains.rider.util.reactive.fire
import kotlin.test.assertEquals
import kotlin.test.Test

class SignalTest {
    @Test
    fun testAdvise() {
        var acc = 0

        val signal : ISignal<Int> = Signal()
        signal.fire(++acc)

        val log = arrayListOf<Int>()
        Lifetime.using { lf ->
            signal.advise(lf, {log.add(it)} )
            lf += {log.add(0)}

            signal.fire(++acc)
            signal.fire(++acc)
        }
        signal.fire(++acc)

        assertEquals(listOf(2, 3, 0), log)
    }

    @Test
    fun testPriorityAdvise() {
        val signal  = Signal<Unit>()
        val log = mutableListOf<Int>()
        signal.adviseEternal { log.add(1) }
        signal.adviseEternal { log.add(2) }

        Signal.priorityAdviseSection {
            signal.adviseEternal { log.add(3) }
            signal.adviseEternal { log.add(4) }
        }

        signal.adviseEternal { log.add(5) }


        signal.fire()
        assertEquals(listOf(3, 4, 1, 2, 5), log)
    }


}