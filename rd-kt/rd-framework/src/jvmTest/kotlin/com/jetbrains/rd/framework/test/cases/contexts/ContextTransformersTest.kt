package com.jetbrains.rd.framework.test.cases.contexts

import com.jetbrains.rd.framework.FrameworkMarshallers
import com.jetbrains.rd.framework.RdContextKey
import com.jetbrains.rd.framework.impl.ContextValueTransformer
import com.jetbrains.rd.framework.impl.ContextValueTransformerDirection
import com.jetbrains.rd.framework.impl.RdPerContextMap
import com.jetbrains.rd.framework.impl.RdSignal
import com.jetbrains.rd.framework.test.util.RdFrameworkTestBase
import org.junit.Assert
import org.junit.Test
import org.junit.experimental.theories.DataPoint
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith

@RunWith(Theories::class)
class ContextTransformersTest : RdFrameworkTestBase() {
    companion object {
        @DataPoint
        @JvmField
        val trueValue = true

        @DataPoint
        @JvmField
        val falseValue = false
    }

    @Test
    fun testBasic() {
        val key = RdContextKey<String>("test-key", true, FrameworkMarshallers.String)

        serverProtocol.contextHandler.registerKey(key)
        clientProtocol.contextHandler.registerKey(key)

        serverProtocol.contextHandler.setTransformerForKey(key) { value, dir ->
            value ?: return@setTransformerForKey null
            if (dir == ContextValueTransformerDirection.ReadFromProtocol)
                return@setTransformerForKey (value.toInt() - 3).toString()
            return@setTransformerForKey (value.toInt() + 3).toString()
        }

        serverProtocol.contextHandler.getValueSet(key).addAll(listOf("1", "2", "3"))

        Assert.assertEquals(setOf("4", "5", "6"), clientProtocol.contextHandler.getValueSet(key).toSet())
    }

    @Theory
    fun testRW(heavy: Boolean, isRead: Boolean) {
        println("Heavy: $heavy isRead: $isRead")
        val key = RdContextKey("test-key", heavy, FrameworkMarshallers.String)

        val serverSignal = RdSignal<String>()
        val clientSignal = RdSignal<String>()

        serverProtocol.bindStatic(serverSignal, 1)
        clientProtocol.bindStatic(clientSignal, 1)

        serverProtocol.contextHandler.registerKey(key)
        clientProtocol.contextHandler.registerKey(key)

        (if(isRead) clientProtocol else serverProtocol).contextHandler.setTransformerForKey(key) { value, dir ->
            value ?: return@setTransformerForKey null
            if ((dir == ContextValueTransformerDirection.ReadFromProtocol) != isRead)
                return@setTransformerForKey (value.toInt() - 3).toString()
            return@setTransformerForKey (value.toInt() + 3).toString()
        }

        key.value = "1"

        clientSignal.advise(serverLifetime) {
            assert(key.value == "4")
        }

        serverSignal.fire("")

        assert(key.value == "1")
    }

    @Test
    fun testLateSet() {
        val key = RdContextKey<String>("test-key", true, FrameworkMarshallers.String)

        serverProtocol.contextHandler.registerKey(key)
        clientProtocol.contextHandler.registerKey(key)

        serverProtocol.contextHandler.getValueSet(key).addAll(listOf("1", "2", "3"))

        Assert.assertEquals(setOf("1", "2", "3"), clientProtocol.contextHandler.getValueSet(key).toSet())

        serverProtocol.contextHandler.setTransformerForKey(key) { value, dir ->
            value ?: return@setTransformerForKey null
            if (dir == ContextValueTransformerDirection.ReadFromProtocol)
                return@setTransformerForKey (value.toInt() - 3).toString()
            return@setTransformerForKey (value.toInt() + 3).toString()
        }

        Assert.assertEquals(setOf("1", "2", "3"), clientProtocol.contextHandler.getValueSet(key).toSet())
        Assert.assertEquals(setOf("-2", "-1", "0"), serverProtocol.contextHandler.getValueSet(key).toSet())
    }

    @Test
    fun testWithContextMap() {
        val key = RdContextKey<String>("test-key", true, FrameworkMarshallers.String)

        serverProtocol.contextHandler.registerKey(key)
        clientProtocol.contextHandler.registerKey(key)

        val serverMap = RdPerContextMap(key) { RdSignal<String>() }
        val clientMap = RdPerContextMap(key) { RdSignal<String>() }

        serverProtocol.bindStatic(serverMap, 1)
        clientProtocol.bindStatic(clientMap, 1)

        val transformer: ContextValueTransformer<String> = setTransformerForKey@{ value, dir ->
            value ?: return@setTransformerForKey null
            if (dir == ContextValueTransformerDirection.ReadFromProtocol)
                return@setTransformerForKey (value.toInt() - 3).toString()
            return@setTransformerForKey (value.toInt() + 3).toString()
        }
        serverProtocol.contextHandler.setTransformerForKey(key, transformer)

        serverProtocol.contextHandler.getValueSet(key).addAll(listOf("1", "2", "3"))
        var receives = 0

        clientMap.view(clientLifetime) { elt, k, v ->
            v.advise(elt) {
                assert(transformer(it, ContextValueTransformerDirection.WriteToProtocol) == k)
                receives++
            }
        }

        serverMap.view(serverLifetime) { _, k, v ->
            v.fire(k)
        }

        Assert.assertEquals(3, receives)
    }

    @Test
    fun testWithTwoKeys() {
        val key1 = RdContextKey<String>("test-key1", true, FrameworkMarshallers.String)
        val key2 = RdContextKey<String>("test-key2", true, FrameworkMarshallers.String)

        serverProtocol.contextHandler.registerKey(key1)
        clientProtocol.contextHandler.registerKey(key1)

        serverProtocol.contextHandler.registerKey(key2)
        clientProtocol.contextHandler.registerKey(key2)

        serverProtocol.contextHandler.setTransformerForKey(key1) { value, dir ->
            value ?: return@setTransformerForKey null
            if (dir == ContextValueTransformerDirection.ReadFromProtocol)
                return@setTransformerForKey (value.toInt() - 3).toString()
            return@setTransformerForKey (value.toInt() + 3).toString()
        }

        serverProtocol.contextHandler.setTransformerForKey(key2) { value, dir ->
            value ?: return@setTransformerForKey null
            if (dir == ContextValueTransformerDirection.ReadFromProtocol)
                return@setTransformerForKey (value.toInt() - 10).toString()
            return@setTransformerForKey (value.toInt() + 10).toString()
        }

        key1.value = "1"
        key2.value = "2"

        serverProtocol.contextHandler.getValueSet(key1).addAll(listOf("1", "2", "3"))
        Assert.assertEquals(setOf("4", "5", "6"), clientProtocol.contextHandler.getValueSet(key1).toSet())

        serverProtocol.contextHandler.getValueSet(key2).addAll(listOf("1", "2", "3"))
        Assert.assertEquals(setOf("11", "12", "13"), clientProtocol.contextHandler.getValueSet(key2).toSet())

        serverProtocol.contextHandler.getValueSet(key1).addAll(listOf("9", "10", "11"))
        Assert.assertEquals(setOf("4", "5", "6", "12", "13", "14"), clientProtocol.contextHandler.getValueSet(key1).toSet())

        serverProtocol.contextHandler.getValueSet(key2).addAll(listOf("9", "10", "11"))
        Assert.assertEquals(setOf("11", "12", "13", "19", "20", "21"), clientProtocol.contextHandler.getValueSet(key2).toSet())

        serverProtocol.contextHandler.getValueSet(key1).clear()
        Assert.assertTrue(clientProtocol.contextHandler.getValueSet(key1).isEmpty())
    }
}