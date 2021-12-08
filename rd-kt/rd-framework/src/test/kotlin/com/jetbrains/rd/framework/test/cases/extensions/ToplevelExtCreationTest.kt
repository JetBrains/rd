package com.jetbrains.rd.framework.test.cases.extensions

import com.jetbrains.rd.framework.test.util.RdFrameworkTestBase
import demo.DemoModel
import demo.demoModel
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class ToplevelExtCreationTest : RdFrameworkTestBase() {
    @Test
    fun testCreateOrThrow() {
        serverProtocol.createExtensionOrThrow(DemoModel::class) { DemoModel.createModel(serverProtocol.lifetime, serverProtocol) }
        assertThrows<IllegalStateException> {
            serverProtocol.createExtensionOrThrow(DemoModel::class) { DemoModel.createModel(serverProtocol.lifetime, serverProtocol) }
        }
    }
    
    @Test
    fun testGetOrNull() {
        assertNull(DemoModel.getOrNull(serverProtocol))
        assertNotNull(serverProtocol.demoModel)
        assertNotNull(DemoModel.getOrNull(serverProtocol))
    }
    
    @Test
    fun testGetOrCreate() {
        val model1 = assertNotNull(serverProtocol.demoModel)
        val model2 = assertNotNull(serverProtocol.demoModel)
        assertSame(model1, model2)
    }
}