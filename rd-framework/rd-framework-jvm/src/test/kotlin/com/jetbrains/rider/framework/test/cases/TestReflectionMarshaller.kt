package com.jetbrains.rider.framework.test.cases

import com.jetbrains.rider.framework.*
import com.jetbrains.rider.framework.base.IRdReactive
import com.jetbrains.rider.framework.base.RdBindableBase
import com.jetbrains.rider.framework.base.RdReactiveBase
import com.jetbrains.rider.framework.base.withId
import com.jetbrains.rider.framework.impl.RdProperty
import com.jetbrains.rider.framework.impl.RdSignal
import com.jetbrains.rider.framework.test.util.TestWire
import com.jetbrains.rider.util.threading.SynchronousScheduler
import org.junit.Before
import org.junit.Test
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.test.assertEquals

class TestReflectionMarshaller {


    val ctx = SerializationCtx(Protocol(Serializers(),
            Identities(),
            SynchronousScheduler,
            TestWire(SynchronousScheduler))
    )
    val buf = createAbstractBuffer()

    @Before
    fun setup() {
        buf.rewind()
    }

    private inline fun <reified T:Any> assertRdEquals(orig: T, new: T) {
        val cl = orig::class as KClass<T>
        for (p in cl.memberProperties) {
            val (o, n) = p.get(orig) to p.get(new)
            if (o is IRdReactive && n is IRdReactive) {
                //todo assert state is equal
                assertEquals(o.rdid, n.rdid)
            } else {
                assertEquals(o, n)
            }
        }
    }

    private inline fun <reified T: Any> doTest(orig : T) {
        buf.rewind()
        val m = ReflectionMarshaller<T>()
        m.write(ctx, buf, orig)

        buf.rewind()
        val new = m.read(ctx, buf)

        if (orig is RdReactiveBase && new is RdReactiveBase) {
            assertRdEquals(orig.rdid, new.rdid)
        }
        assertRdEquals(orig, new)
    }

    @Test
    fun testEmpty() {
        class X() {
            override fun equals(other: Any?) = other is X
        }
        doTest(X())
    }

    @Test
    fun testOneInt() {
        data class X(val x: Int)
        doTest(X(42))
        doTest(X(0))
    }

    @Test
    fun testIntAndString() {
        data class X(val x: Int, val y: String)
        doTest(X(42, "hey"))
        doTest(X(43, ""))
    }

    @Test
    fun testNullableIntAndString() {
        data class X(val x: Int?, val y: String?)
        doTest(X(42, "hey"))
        doTest(X(43, ""))
        doTest(X(44, null))
        doTest(X(0, null))
        doTest(X(null, null))
        doTest(X(null, ""))
        doTest(X(null, "abc"))
    }

    @Test
    fun testLists() {
        data class X(val x: List<Int>, val y: List<Int?>, val z:List<Int?>?)
        doTest(X(emptyList(), emptyList(), null))
        doTest(X(emptyList(), emptyList(), emptyList()))
        doTest(X(listOf(1), listOf(null), listOf(null, 1)))
    }

    @Test
    fun testRdEntities() {
        data class X(val x: RdSignal<Int>, val y:RdProperty<String>)
        doTest(X(RdSignal<Int>().withId(RdId(1L)), RdProperty("a").withId(RdId(2L))))

    }
}