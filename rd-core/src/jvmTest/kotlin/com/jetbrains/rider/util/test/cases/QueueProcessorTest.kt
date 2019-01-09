package com.jetbrains.rider.util.test.cases

import com.jetbrains.rider.util.lifetime.LifetimeDefinition
import com.jetbrains.rider.util.threading.QueueProcessor
import org.junit.Test
import kotlin.test.assertEquals

class QueueProcessorTest {

    @Test
    fun test1() {
        val lt = LifetimeDefinition()
        val log = mutableListOf<Int>()
        val proc = QueueProcessor<Int>(lt) {
            log.add(it)
        }

        proc.queue(1)
        proc.queue(2)
        proc.queue(3)
        Thread.sleep(100)


        lt.terminate()

        proc.queue(4)
        proc.queue(5)
        Thread.sleep(100)

        assertEquals(log, listOf(1,2,3))
    }
}