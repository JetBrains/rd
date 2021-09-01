package com.jetbrains.rd.framework.test.cases.serialization

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.IRdReactive
import com.jetbrains.rd.framework.base.RdReactiveBase
import com.jetbrains.rd.framework.base.withId
import com.jetbrains.rd.framework.impl.RdProperty
import com.jetbrains.rd.framework.impl.RdSignal
import com.jetbrains.rd.framework.test.util.TestWire
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.threading.SynchronousScheduler
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

class TestReflectionMarshaller {

    companion object {
        val lifetimeDef = Lifetime.Eternal.createNested()

        @AfterAll
        @JvmStatic
        fun afterTest() {
            lifetimeDef.terminate()
        }
    }


    val ctx = SerializationCtx(Protocol("TestReflection", Serializers(),
            Identities(IdKind.Client),
            SynchronousScheduler,
            TestWire(SynchronousScheduler), lifetimeDef.lifetime)
    )
    val buf = createAbstractBuffer()

    @BeforeEach
    fun setup() {
        buf.rewind()
    }

    private inline fun <reified T:Any> assertRdEquals(orig: T, new: T) {
        @Suppress("UNCHECKED_CAST") val cl = orig::class as KClass<T>
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
        class X {
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