package com.jetbrains.rd.framework.test.cases.contexts

import com.jetbrains.rd.framework.FrameworkMarshallers
import com.jetbrains.rd.framework.RdContext
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

    object TestKeyHeavy : RdContext<String>("test-key", true, FrameworkMarshallers.String)
    object TestKeyLight : RdContext<String>("test-key", false, FrameworkMarshallers.String)

    @Theory
    fun testLateAdd(heavy: Boolean) {
        println("Heavy: $heavy")
        val key = if(heavy) TestKeyHeavy else TestKeyLight

        val serverSignal = RdSignal<String>()
        val clientSignal = RdSignal<String>()

        serverProtocol.bindStatic(serverSignal, 1)
        clientProtocol.bindStatic(clientSignal, 1)

        serverProtocol.contexts.registerContext(key)

        key.value = "1"

        Lifetime.using { lt ->
            var fired = false
            clientSignal.advise(lt) {
                assert(key.value == "1")
                fired = true
            }

            serverSignal.fire("")
            assert(fired)
        }

        clientProtocol.contexts.registerContext(key)

        Lifetime.using { lt ->
            var fired = false
            clientSignal.advise(lt) {
                assert(key.value == "1")
                fired = true
            }

            serverSignal.fire("")
            assert(fired)
        }

        assert(key.value == "1")

        Lifetime.using { lt ->
            var fired = false
            serverSignal.advise(lt) {
                assert(key.value == "1")
                fired = true
            }

            clientSignal.fire("")
            assert(fired)
        }

        assert(key.value == "1")
    }
}