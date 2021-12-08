package com.jetbrains.rd.framework.test.cases.extensions

import com.jetbrains.rd.framework.test.util.RdFrameworkTestBase
import demo.DemoModel
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class ToplevelExtCreationTest : RdFrameworkTestBase() {
    @Test
    fun testCreateOrThrow() {
        DemoModel.createOrThrow(serverProtocol)
        assertThrows<IllegalStateException> {
            DemoModel.createOrThrow(serverProtocol)
        }
    }
    
    @Test
    fun testGetOrNull() {
        assertNull(DemoModel.getOrNull(serverProtocol))
        assertNotNull(DemoModel.getOrCreate(serverProtocol))
        assertNotNull(DemoModel.getOrNull(serverProtocol))
    }
    
    @Test
    fun testGetOrCreate() {
        val model1 = assertNotNull(DemoModel.getOrCreate(serverProtocol))
        val model2 = assertNotNull(DemoModel.getOrCreate(serverProtocol))
        assertSame(model1, model2)
    }
}