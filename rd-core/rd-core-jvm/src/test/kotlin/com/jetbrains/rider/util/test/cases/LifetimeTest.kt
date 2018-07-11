package com.jetbrains.rider.util.test.cases

import com.jetbrains.rider.util.lifetime2.RLifetimeDef
import com.jetbrains.rider.util.lifetime2.defineNested
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LifetimeTest {

    @Test
    fun testEmptyLifetime() {
        val def = RLifetimeDef()
        assertTrue { def.terminate() }

        assertFalse { def.terminate() }
        assertFalse { def.terminate() }
    }

    @Test
    fun testActionsSequence() {
        val log = mutableListOf<Int>()

        val def = RLifetimeDef()
        def.onTermination { log.add(1) }
        def.onTermination { log.add(2) }
        def.onTermination { log.add(3) }

        def.terminate()

        assertEquals(listOf(3, 2, 1), log)
    }

    @Test
    fun testNestedLifetime() {
        val log = mutableListOf<Int>()

        val def = RLifetimeDef()
        def.onTermination { log.add(1) }
        def.defineNested().onTermination { log.add(2) }
        def.onTermination { log.add(3) }

        def.terminate()

        assertEquals(listOf(3, 2, 1), log)
    }
}