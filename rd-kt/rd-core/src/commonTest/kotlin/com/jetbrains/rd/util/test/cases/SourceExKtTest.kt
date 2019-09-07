package com.jetbrains.rd.util.test.cases

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.*
import com.jetbrains.rd.util.test.framework.RdTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SourceExKtTest : RdTestBase()  {
    @Test
    fun testAdviseUntil() {
        val def = Lifetime.Eternal.createNested()
        val lifetime = def.lifetime
        val signal: ISignal<Int> = Signal()

        var cur = 0
        signal.adviseUntil(lifetime) { value ->
            cur = value
            value < 0
        }

        assertEquals(0, cur)

        signal.fire(1)
        assertEquals(1, cur)

        signal.fire(2)
        assertEquals(2, cur)

        signal.fire(-1)
        assertEquals(-1, cur)

        signal.fire(4)
        assertEquals(-1, cur)
    }

    @Test
    fun testAdviseNotNull() {
        class Box(val value: Int)

        val def = Lifetime.Eternal.createNested()
        val lifetime = def.lifetime
        val signal: ISignal<Box?> = Signal()

        var cur = 0
        signal.adviseNotNull(lifetime) { a ->
            cur = a.value
        }

        assertEquals(0, cur)

        signal.fire(null)
        assertEquals(0, cur)

        signal.fire(Box(2))
        assertEquals(2, cur)

        signal.fire(null)
        assertEquals(2, cur)

        signal.fire(Box(4))
        assertEquals(4, cur)
    }

    @Test
    fun testAdviseNotNullOnce() {
        class Box(val value: Int)

        val def = Lifetime.Eternal.createNested()
        val lifetime = def.lifetime
        val signal = Signal<Box?>()

        var cur = 0
        signal.adviseNotNullOnce(lifetime) { a ->
            cur = a.value
        }

        assertEquals(0, cur)

        signal.fire(null)
        assertEquals(0, cur)

        signal.fire(Box(2))
        assertEquals(2, cur)

        signal.fire(Box(3))
        assertEquals(2, cur)
    }

    @Test
    fun testFlowInto() {
        val def = Lifetime.Eternal.createNested()
        val lifetime = def.lifetime
        val signal: ISignal<Int> = Signal()
        val signalDependent: ISignal<Boolean> = Signal()

        signal.flowInto(lifetime, signalDependent) { v -> v >= 0 }

        var acc = false
        signalDependent.advise(lifetime) { v -> acc = v }

        assertFalse(acc)

        signal.fire(-1)
        assertFalse(acc)

        signal.fire(+1)
        assertTrue(acc)

        signal.fire(+100)
        assertTrue(acc)

        signal.fire(-100)
        assertFalse(acc)
    }

    @Test
    fun testFlowInto1() {
        val def = Lifetime.Eternal.createNested()
        val lifetime = def.lifetime
        val signal: ISignal<Int> = Signal()
        val signalDependentA: ISignal<Int> = Signal()
        val signalDependentB: ISignal<Int> = Signal()

        var accA = 0
        var accB = 0

        signalDependentA.advise(lifetime) { v -> accA = v }
        signalDependentB.advise(lifetime) { v -> accB = v }

        val defFlow = lifetime.createNested()
        val lifetimeFlow = defFlow.lifetime
        signal.flowInto(lifetimeFlow, signalDependentA)
        signal.flowInto(lifetime, signalDependentB)

        signal.fire(1)

        assertEquals(1, accA)
        assertEquals(1, accB)

        defFlow.terminate()

        signal.fire(0)

        assertEquals(1, accA)
        assertEquals(0, accB)
    }

    @Test
    fun testMap() {
        val def = Lifetime.Eternal.createNested()
        val lifetime = def.lifetime
        val signal: ISignal<Int> = Signal()

        val log = ArrayList<Int>()
        signal.advise(lifetime) { v -> log.add(v) }

        signal.fire(0)

        val newSignal = signal.map { v -> v >= 0 }
        val logBoolean = ArrayList<Boolean>()

        val newDef = Lifetime.Eternal.createNested()
        val newLifetime = newDef.lifetime

        newSignal.advise(newLifetime) { v -> logBoolean.add(v) }

        signal.fire(1)
        signal.fire(2)
        signal.fire(-1)
        signal.fire(100)

        assertEquals(listOf(0, 1, 2, -1, 100), log)
        assertEquals(listOf(true, true, false, true), logBoolean)
    }
}