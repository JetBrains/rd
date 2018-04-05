package com.jetbrains.rider.framework.test.cases

import com.jetbrains.rider.framework.*
import com.jetbrains.rider.framework.base.RdBindableBase
import com.jetbrains.rider.framework.impl.RdProperty
import com.jetbrains.rider.framework.test.util.TestWire
import com.jetbrains.rider.util.reactive.IProperty
import com.jetbrains.rider.util.threading.SynchronousScheduler
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import kotlin.test.assertEquals

class ReflectedBindable : RdBindableBase() {
    val s : IProperty<String> by RdProperty("")
    val i : List<ReflectedBindable?> by mutableListOf<ReflectedBindable?>()
}

class TestReflect {

    val ctx = SerializationCtx(Protocol(Serializers(),
            Identities(),
            SynchronousScheduler,
            TestWire(SynchronousScheduler))
    )
    val buf = createAbstractBuffer()

    @BeforeMethod
    fun setup() {
        buf.rewind()
    }

    @Test
    fun testEmpty() {
        class Empty() {
            override fun equals(other: Any?) = other is Empty
        }

        val m = ReflectionMarshaller<Empty>()
        val orig = Empty()
        m.write(ctx, buf, orig)

        buf.rewind()
        val new = m.read(ctx, buf)

        assertEquals(orig, new)
    }

    @Test
    fun testOneInt() {
        data class OneInt(val x: Int)

        val m = ReflectionMarshaller<OneInt>()
        val orig = OneInt(42)
        m.write(ctx, buf, orig)

        buf.rewind()
        val new = m.read(ctx, buf)

        assertEquals(orig, new)
    }

    @Test
    fun testIntAndString() {
        data class IntString(val x: Int, val y: String)

        val m = ReflectionMarshaller<IntString>()
        val orig = IntString(42, "hey")
        m.write(ctx, buf, orig)

        buf.rewind()
        val new = m.read(ctx, buf)

        assertEquals(orig, new)
    }
}