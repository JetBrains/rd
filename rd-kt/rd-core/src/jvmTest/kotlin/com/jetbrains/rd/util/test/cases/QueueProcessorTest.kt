package com.jetbrains.rd.util.test.cases

import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.threading.QueueProcessor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@ObsoleteCoroutinesApi
class QueueProcessorTest {

    @ExperimentalCoroutinesApi
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