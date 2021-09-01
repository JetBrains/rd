package com.jetbrains.rd.util.test.cases

import com.jetbrains.rd.util.lifetime.*
import com.jetbrains.rd.util.test.framework.RdTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SequentialLifetimesTest : RdTestBase()  {
    @Test
    fun testContract() {
        val seq = SequentialLifetimes(Lifetime.Eternal)

        assertTrue(seq.isTerminated)

        val parentDef = Lifetime.Eternal.createNested()
        val seq2 = SequentialLifetimes(parentDef.lifetime)
        val nextLt = seq2.next()
        parentDef.terminate()

        assertTrue(!nextLt.isAlive)
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
        val def = Lifetime.Eternal.createNested()

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
        val defA = Lifetime.Eternal.createNested()
        val defB = Lifetime.Eternal.createNested()

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

    @Test
    fun testTerminateCurrent01() {
        val sequentialLifetimes = SequentialLifetimes(Lifetime.Eternal)
        sequentialLifetimes.defineNext { _, lifetime ->
            lifetime.onTermination {
                assertTrue(lifetime.isNotAlive, "lifetime.isNotAlive")
                assertTrue(sequentialLifetimes.isTerminated, "sequentialLifetimes.isTerminated")
            }
        }
        sequentialLifetimes.terminateCurrent()
    }

    @Test
    fun testTerminateCurrent02() {
        val sb = StringBuilder()
        val sequentialLifetimes = SequentialLifetimes(Lifetime.Eternal)
        sequentialLifetimes.defineNext { _, lifetime ->
            lifetime.onTermination {
                sb.append("T1")
                assertTrue(lifetime.isNotAlive, "lifetime.isNotAlive")
                assertTrue(sequentialLifetimes.isTerminated, "sequentialLifetimes.isTerminated")
            }
        }
        sequentialLifetimes.defineNext { _, lifetime ->
            sb.append("N2")
        }

        assertEquals("T1N2", sb.toString())
    }
}

