package com.jetbrains.rider.util.test.cases

import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.reactive.OptProperty
import com.jetbrains.rider.util.reactive.Property
import com.jetbrains.rider.util.reactive.compose
import com.jetbrains.rider.util.reactive.map
import kotlin.test.Test
import kotlin.test.assertEquals


class PropertyCompositionTest {
    @Test
    fun composeTest() {
        val targetLog = mutableListOf<Int>()
        val targetChangeLog = mutableListOf<Int>()
        Lifetime.using { lifetime ->
            val left = Property(0)
            val right = Property(0)
            val target = left.compose(right) { l, r -> l / 2 + r / 2 }
            target.advise(lifetime) {
                targetLog.add(it)
            }
            target.change.advise(lifetime) {
                targetChangeLog.add(it)
            }
            left.value = 1
            right.value = 1
            left.value = 2
            right.value = 2
        }
        assertEquals(targetChangeLog, listOf(1, 2), "target.change log ${targetChangeLog.joinToString()}")
        assertEquals(targetLog, listOf(0, 1, 2), "target log ${targetLog.joinToString()}")
    }

    @Test
    fun optComposeTest() {
        val targetLog = mutableListOf<Int>()
        val targetChangeLog = mutableListOf<Int>()
        Lifetime.using { lifetime ->
            val left = OptProperty<Int>()
            val right = OptProperty<Int>()
            val target = left.compose(right) { l, r -> l / 2 + r / 2 }
            target.advise(lifetime) {
                targetLog.add(it)
            }
            target.change.advise(lifetime) {
                targetChangeLog.add(it)
            }
            left.set(0)
            right.set(0)
            left.set(1)
            right.set(1)
            left.set(2)
            right.set(2)
        }
        assertEquals(targetChangeLog, listOf(0, 1, 2), "target.change log ${targetChangeLog.joinToString()}")
        assertEquals(targetLog, listOf(0, 1, 2), "target log ${targetLog.joinToString()}")
    }

    @Test
    fun mapTest() {
        val targetLog = mutableListOf<Int>()
        Lifetime.using { lifetime ->
            val source = Property(0)
            val target = source.map { it / 2 }
            target.advise(lifetime) {
                targetLog.add(it)
            }
            source.value = 1
            source.value = 2
            source.value = 3
            source.value = 4
        }
        assertEquals(targetLog, listOf(0, 1, 2), "target log ${targetLog.joinToString()}")
    }

    @Test
    fun optMapTest() {
        val targetLog = mutableListOf<Int>()
        Lifetime.using { lifetime ->
            val source = OptProperty<Int>()
            val target = source.map { it / 2 }
            target.advise(lifetime) {
                targetLog.add(it)
            }
            for (i in 0..4) {
                source.set(i)
            }
        }
        assertEquals(targetLog, listOf(0, 1, 2), "target log ${targetLog.joinToString()}")
    }
}
