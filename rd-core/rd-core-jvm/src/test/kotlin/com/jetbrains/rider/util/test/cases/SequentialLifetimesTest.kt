package com.jetbrains.rider.util.test.cases

import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.lifetime.LifetimeDefinition
import com.jetbrains.rider.util.lifetime.SequentialLifetimes
import com.jetbrains.rider.util.lifetime.plusAssign
import org.testng.annotations.Test
import kotlin.test.assertEquals

class SequentialLifetimesTest{
    @Test
    fun testContract() {
        val seq = SequentialLifetimes(Lifetime.Eternal)

        assert(seq.isTerminated)

        val parentDef = Lifetime.create(Lifetime.Eternal)
        val seq2 = SequentialLifetimes(parentDef.lifetime)
        val nextLt = seq2.next()
        parentDef.terminate()

        assert(nextLt.isTerminated)
        assert(seq2.isTerminated)
    }

    @Test
    fun testEternal() {
        val seq = SequentialLifetimes(Lifetime.Eternal)

        var acc = 0
        val lf = seq.next()
        lf += {acc++}
        lf += {acc++}
        assertEquals(0, acc)

        LifetimeDefinition.Eternal.terminate(); //bullshit
        assertEquals(0, acc)

        seq.next() += {acc++}
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
        lf += {acc++}
        lf += {acc++}
        assertEquals(0, acc)

        LifetimeDefinition.Eternal.terminate() //bullshit
        assertEquals(0, acc)

        seq.next() += {acc++}
        assertEquals(2, acc)

        def.terminate()
        assertEquals(3, acc)
    }
}

