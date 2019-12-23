package com.jetbrains.rd.framework.test.cases.contexts

import com.jetbrains.rd.framework.RdContext
import com.jetbrains.rd.framework.test.util.RdFrameworkTestBase
import demo.DemoModel
import demo.extModel
import org.junit.Assert
import org.junit.experimental.theories.DataPoint
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith

@RunWith(Theories::class)
class ContextWithExtTest : RdFrameworkTestBase() {
    companion object {
        @JvmField
        @DataPoint
        val trueValue = true
        @JvmField
        @DataPoint
        val falseValue = false
    }

    @Theory
    fun testExtPreserveContextOnLateConnect(useHeavyContext: Boolean) {
        println("useHeavyContext: $useHeavyContext")
        val context = if(useHeavyContext) ContextsTest.TestKeyHeavy else ContextsTest.TestKeyLight
        clientProtocol.serializers.register(RdContext.marshallerFor(context))
        serverProtocol.contexts.registerContext(context)
        
        val serverModel = DemoModel.create(serverLifetime, serverProtocol)
        val clientModel = DemoModel.create(clientLifetime, clientProtocol)

        val serverExt = serverModel.extModel

        val fireValues = listOf("a", "b", "c")

        setWireAutoFlush(false)

        fireValues.forEach {
            context.value = it
            serverExt.checker.fire(Unit)
            context.value = null
        }

        var numReceives = 0
        val receivedContexts = mutableSetOf<String>()

        val clientExt = clientModel.extModel
        clientExt.checker.advise(clientLifetime) {
            numReceives++
            receivedContexts.add(context.value ?: "null")
        }

        setWireAutoFlush(true)

        Assert.assertEquals(3, numReceives)
        Assert.assertEquals(fireValues.toSet(), receivedContexts)
    }
}