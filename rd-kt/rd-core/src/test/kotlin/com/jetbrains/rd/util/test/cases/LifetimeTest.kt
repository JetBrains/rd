package com.jetbrains.rd.util.test.cases

import com.jetbrains.rd.util.lifetime.*
import com.jetbrains.rd.util.log.ErrorAccumulatorLoggerFactory
import com.jetbrains.rd.util.test.framework.RdTestBase
import com.jetbrains.rd.util.threading.SpinWait
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.provider.EnumSource
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

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

                    SpinWait.spinUntil { def.status == LifetimeStatus.Canceling }
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

    @Test
    fun nestedLifetimesLeakageTest() {
        var p1 = false
        var p2 = false
        var p3 = false
        Lifetime.using { lifetime ->
            var prev: LifetimeDefinition? = null

            lifetime.onTermination { p1 = true }
            for (i in 0..10_000) {
                lifetime.usingNested {
                    if (i == 5000)
                        lifetime.onTermination { p2 = true }

                    prev?.terminate()
                    prev = lifetime.createNested()

                    if (i == 7000)
                        lifetime.onTermination { p3 = true }
                }
            }

            val r = lifetime::class.declaredMemberProperties.single { it.name == "resources" }
            r.isAccessible = true
            val resources = r.getter.call(lifetime) as Array<*>
            assert(resources.size <= 12)
        }

        assert(p1) { "p1" }
        assert(p2) { "p2" }
        assert(p3) { "p3" }
    }

    @Test
    fun testTerminationTimeout() {
        val defA = LifetimeDefinition(testLifetime).apply { terminationTimeoutKind = LifetimeTerminationTimeoutKind.Long }
        val defB = LifetimeDefinition(defA.lifetime)
        val defC = LifetimeDefinition(defA.lifetime).apply { terminationTimeoutKind = LifetimeTerminationTimeoutKind.Short }

        assertEquals(LifetimeTerminationTimeoutKind.Long, defB.terminationTimeoutKind)
        assertEquals(LifetimeTerminationTimeoutKind.Long, defA.terminationTimeoutKind)
        assertEquals(LifetimeTerminationTimeoutKind.Short, defC.terminationTimeoutKind)
    }

    @Test
    fun testSetTestTerminationTimeout() {
        enumValues<LifetimeTerminationTimeoutKind>().forEach { timeoutKind ->

            val oldTimeoutMs = Lifetime.getTerminationTimeoutMs(timeoutKind)
            try {
                Lifetime.setTerminationTimeoutMs(timeoutKind, 2000)

                val subDef = LifetimeDefinition(testLifetime).apply { terminationTimeoutKind = timeoutKind }
                val subLt = subDef.lifetime;

                val future = CompletableFuture<Unit>()
                thread {
                    subLt.executeIfAlive {
                        future.complete(Unit)
                        Thread.sleep(750)
                    }
                }
                assertNotNull(future.get(1, TimeUnit.SECONDS))
                subDef.terminate()

                assertDoesNotThrow { ErrorAccumulatorLoggerFactory.throwAndClear() }
            } finally {
                Lifetime.setTerminationTimeoutMs(timeoutKind, oldTimeoutMs)
            }

        }
    }
}