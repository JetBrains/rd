package com.jetbrains.rd.util.test.cases

import com.jetbrains.rd.util.lifetime.*
import com.jetbrains.rd.util.test.framework.RdTestBase
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible
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
        val lf = seq.next().lifetime
        lf.plusAssign { acc++ }
        lf.plusAssign { acc++ }
        assertEquals(0, acc)

        assertEquals(0, acc)

        seq.next().lifetime.plusAssign { acc++ }
        assertEquals(2, acc)

        seq.terminateCurrent()
        assertEquals(3, acc)
    }

    @Test
    fun testNonEternal() {
        val def = Lifetime.Eternal.createNested()

        val seq = SequentialLifetimes(def.lifetime)

        var acc = 0
        val lf = seq.next().lifetime
        lf.plusAssign { acc++ }
        lf.plusAssign { acc++ }
        assertEquals(0, acc)

        assertEquals(0, acc)

        seq.next().lifetime.plusAssign { acc++ }
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

        lfA.lifetime.plusAssign { accA = true }
        lfB.lifetime.plusAssign { accB = true }

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

    @Test
    fun nestedLifetimesLeakageTest() {
        var p1 = false
        var p2 = false
        var p3 = false
        Lifetime.using { lifetime ->
            val seq = SequentialLifetimes(lifetime)

            var prev: LifetimeDefinition? = null

            lifetime.onTermination { p1 = true }
            for (i in 0..10_000) {
                seq.next()

                if (i == 5000)
                    lifetime.onTermination { p2 = true }

                prev?.terminate()
                prev = lifetime.createNested()

                if (i == 7000)
                    lifetime.onTermination { p3 = true }
            }

            val r = lifetime.definition::class.declaredMemberProperties.single { it.name == "resources" }
            r.isAccessible = true
            val resources = r.getter.call(lifetime.definition) as Array<*>
            assert(resources.size <= 12)
        }

        assert(p1) { "p1" }
        assert(p2) { "p2" }
        assert(p3) { "p3" }
    }
}

