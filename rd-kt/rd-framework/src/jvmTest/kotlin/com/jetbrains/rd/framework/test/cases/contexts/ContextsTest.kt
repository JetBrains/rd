package com.jetbrains.rd.framework.test.cases.contexts

import com.jetbrains.rd.framework.FrameworkMarshallers
import com.jetbrains.rd.framework.RdContextKey
import com.jetbrains.rd.framework.impl.ContextValueTransformerDirection
import com.jetbrains.rd.framework.impl.RdSignal
import com.jetbrains.rd.framework.test.util.RdFrameworkTestBase
import com.jetbrains.rd.util.lifetime.Lifetime
import org.junit.experimental.theories.DataPoint
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith

@RunWith(Theories::class)
class ContextsTest : RdFrameworkTestBase() {
    companion object {
        @JvmField
        @DataPoint
        val trueValue = true
        @JvmField
        @DataPoint
        val falseValue = false
    }

    @Theory
    fun testLateAdd(heavy: Boolean) {
        println("Heavy: $heavy")
        val key = RdContextKey("test-key", heavy, FrameworkMarshallers.String)

        val serverSignal = RdSignal<String>()
        val clientSignal = RdSignal<String>()

        serverProtocol.bindStatic(serverSignal, 1)
        clientProtocol.bindStatic(clientSignal, 1)

        serverProtocol.contextHandler.registerKey(key)

        serverProtocol.contextHandler.setTransformerForKey(key) { value, dir ->
            value ?: return@setTransformerForKey null
            if (dir == ContextValueTransformerDirection.ReadFromProtocol)
                return@setTransformerForKey (value.toInt() - 3).toString()
            return@setTransformerForKey (value.toInt() + 3).toString()
        }

        key.value = "1"

        Lifetime.using { lt ->
            var fired = false
            clientSignal.advise(lt) {
                assert(key.value == "4")
                fired = true
            }

            serverSignal.fire("")
            assert(fired)
        }

        clientProtocol.contextHandler.registerKey(key)

        Lifetime.using { lt ->
            var fired = false
            clientSignal.advise(lt) {
                assert(key.value == "4")
                fired = true
            }

            serverSignal.fire("")
            assert(fired)
        }

        assert(key.value == "1")

        Lifetime.using { lt ->
            var fired = false
            serverSignal.advise(lt) {
                assert(key.value == "-2")
                fired = true
            }

            clientSignal.fire("")
            assert(fired)
        }

        assert(key.value == "1")
    }
}