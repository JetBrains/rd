package com.jetbrains.rider.util.test.cases

import com.jetbrains.rider.util.lifetime2.*
import com.jetbrains.rider.util.test.framework.RdTestBase
import com.jetbrains.rider.util.threading.SpinWait
import org.junit.Test
import kotlin.concurrent.thread
import kotlin.test.*

class LifetimeTest : RdTestBase() {

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

    @Test
    fun testTerminate2Times() {
        val def = RLifetimeDef()
        assertTrue { def.terminate() }
        assertFalse { def.terminate() }
    }

    @Test
    fun testTerminationWithAsyncAction() {
        RLifetime.waitForExecutingInTerminationTimeout = 100000
        val def = RLifetimeDef()
        val log = mutableListOf<Int>()

        val t = thread {
            withFailLog {
                //must execute
                val first = def.executeIfAlive {
                    log.add(0)
                    l11n.point(0)
                    assert(def.status == RLifetimeStatus.Alive)
                    assert(def.isAlive)
                    log.add(1)

                    SpinWait.spinUntil { def.status == RLifetimeStatus.Canceled }
                    assert(!def.isAlive)
                }

                //shoudn't execute
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
        var lf : RLifetime? = null
        RLifetime.using {
            lf = it
            assertTrue { lf!!.isAlive}
            assertFalse {lf!!.isEternal}
        }
        assertFalse { lf!!.isAlive}
    }

    @Test
    fun testEternal() {
        assertTrue { RLifetime.eternal.onTerminationIfAlive {} }
    }


}