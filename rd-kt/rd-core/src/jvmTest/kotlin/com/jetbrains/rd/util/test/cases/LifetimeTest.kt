package com.jetbrains.rd.util.test.cases

import com.jetbrains.rd.util.lifetime.*
import com.jetbrains.rd.util.test.framework.RdTestBase
import com.jetbrains.rd.util.threading.SpinWait
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.concurrent.thread

class LifetimeTest : RdTestBase() {

    @Test
    fun testEmptyLifetime() {
        val def = LifetimeDefinition()
        assertTrue { def.terminate() }

        assertFalse { def.terminate() }
        assertFalse { def.terminate() }
    }

    @Test
    fun testActionsSequence() {
        val log = mutableListOf<Int>()

        val def = LifetimeDefinition()
        def.onTermination { log.add(1) }
        def.onTermination { log.add(2) }
        def.onTermination { log.add(3) }

        def.terminate()

        assertEquals(listOf(3, 2, 1), log)
    }

    @Test
    fun testNestedLifetime() {
        val log = mutableListOf<Int>()

        val def = LifetimeDefinition()
        def.onTermination { log.add(1) }
        def.createNested().onTermination { log.add(2) }
        def.onTermination { log.add(3) }

        def.terminate()

        assertEquals(listOf(3, 2, 1), log)
    }

    @Test
    fun testTerminationWithAsyncAction() {
        Lifetime.waitForExecutingInTerminationTimeout = 100000
        val def = LifetimeDefinition()
        val log = mutableListOf<Int>()

        val t = thread {
            withFailLog {
                //must execute
                val first = def.executeIfAlive {
                    log.add(0)
                    assert(def.status == LifetimeStatus.Alive)
                    assert(def.isAlive)
                    l11n.point(0)
                    log.add(1)

                    SpinWait.spinUntil { def.status == LifetimeStatus.Canceled }
                    assert(!def.isAlive)
                }

                //shouldn't execute
                val second = def.executeIfAlive {
                    log.add(2)
                }

                assertNotNull(first)
                assertNull(second)
            }
        }

        def.onTermination { log.add(-1) }

        l11n.point(1)
        def.terminate()
        l11n.point(2)

        t.join()

        assertEquals(listOf(0, 1, -1), log)
    }


    @Test
    fun testUsing() {
        var lf : Lifetime? = null
        Lifetime.using {
            lf = it
            assertTrue { lf!!.isAlive}
            assertFalse {lf!!.isEternal}
        }
        assertFalse { lf!!.isAlive}
    }

    @Test
    fun testTerminate2Times() {
        val def = LifetimeDefinition()
        assertTrue { def.terminate() }
        assertFalse { def.terminate() }
    }

    @Test
    fun testBracketSuccess() {
        val def = LifetimeDefinition()

        var x = 0
        assertEquals(0, def.bracket({ x++ }, { x++; }))
        assertEquals(1, x)
        def.terminate()
        assertEquals(2, x)
    }

    @Test
    fun testBracketFailure() {
        val def = LifetimeDefinition()

        var x = 0
        assertThrows(Throwable::class.java) { def.bracket({ x++; fail<Throwable>() }, { x++; }) }
        assertEquals(1, x)
        def.terminate()
        assertEquals(1, x)
    }

    @Test
    fun testBracketCanceled() {
        val def = LifetimeDefinition()

        var x = 0
        def.terminate()
        assertNull (def.bracket({ x++; fail<Throwable>() }, { x++; }) )
        assertEquals(0, x)
        def.terminate()
        assertEquals(0, x)
    }

    @Test
    fun testEternal() {
        assertTrue { Lifetime.Eternal.onTerminationIfAlive {} }
    }


}