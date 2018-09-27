package com.jetbrains.rider.util.test.cases

import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.lifetime.LifetimeDefinition
import com.jetbrains.rider.util.lifetime.SequentialLifetimes
import com.jetbrains.rider.util.lifetime.plusAssign
import com.jetbrains.rider.util.test.framework.RdTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SequentialLifetimesTest : RdTestBase()  {
    @Test
    fun testContract() {
        val seq = SequentialLifetimes(Lifetime.Eternal)

        assertTrue(seq.isTerminated)

        val parentDef = Lifetime.create(Lifetime.Eternal)
        val seq2 = SequentialLifetimes(parentDef.lifetime)
        val nextLt = seq2.next()
        parentDef.terminate()

        assertTrue(nextLt.isTerminated)
        assertTrue(seq2.isTerminated)
    }

    @Test
    fun testEternal() {
        val seq = SequentialLifetimes(Lifetime.Eternal)

        var acc = 0
        val lf = seq.next()
        lf.plusAssign { acc++ }
        lf.plusAssign { acc++ }
        assertEquals(0, acc)

        assertEquals(0, acc)

        seq.next().plusAssign { acc++ }
        assertEquals(2, acc)

        seq.terminateCurrent()
        assertEquals(3, acc)
    }

    @Test
    fun testNonEternal() {
        val def = Lifetime.create(Lifetime.Eternal)

        val seq = SequentialLifetimes(def.lifetime)

        var acc = 0
        val lf = seq.next()
        lf.plusAssign { acc++ }
        lf.plusAssign { acc++ }
        assertEquals(0, acc)

        assertEquals(0, acc)

        seq.next().plusAssign { acc++ }
        assertEquals(2, acc)

        def.terminate()
        assertEquals(3, acc)
    }

    @Test
    fun testTwoEternalChildren() {
        val defA = Lifetime.create(Lifetime.Eternal)
        val defB = Lifetime.create(Lifetime.Eternal)

        val seqA = SequentialLifetimes(defA.lifetime)
        val seqB = SequentialLifetimes(defB.lifetime)

        var accA = false
        var accB = false

        val lfA = seqA.next()
        val lfB = seqB.next()

        lfA.plusAssign { accA = true }
        lfB.plusAssign { accB = true }

        lfA.terminate()
        assertTrue(accA)
        assertFalse(accB)

        lfB.terminate()
        assertTrue(accA)
        assertTrue(accB)
    }
}

