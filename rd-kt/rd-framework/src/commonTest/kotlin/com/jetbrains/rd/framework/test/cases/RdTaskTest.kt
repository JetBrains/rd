package com.jetbrains.rd.framework.test.cases

import com.jetbrains.rd.framework.RdTaskResult
import com.jetbrains.rd.framework.base.static
import com.jetbrains.rd.framework.impl.RdCall
import com.jetbrains.rd.framework.isFaulted
import com.jetbrains.rd.framework.test.util.RdFrameworkTestBase
import com.jetbrains.rd.util.reactive.valueOrThrow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class RdTaskTest : RdFrameworkTestBase() {
    @Test
    fun testStaticSuccess() {
        val entity_id = 1

        val client_entity = RdCall<Int, String>().static(entity_id)
        val server_entity = RdCall(null, null, Int::toString).static(entity_id)

        //not bound
        assertFails { client_entity.sync(0) }
        assertFails { client_entity.start(0) }

        clientProtocol.bindStatic(client_entity, "top")
        serverProtocol.bindStatic(server_entity, "top")

        assertEquals("0", client_entity.sync(0))
        assertEquals("1", client_entity.sync(1))

        val taskResult = (client_entity.start(2).result.valueOrThrow as RdTaskResult.Success<String>)
        assertEquals("2", taskResult.value)
    }

    @Test
    fun testStaticFailure() {
        val entity_id = 1

        val client_entity = RdCall<Int, String>().static(entity_id)
        val server_entity = RdCall<Int, String> { _ -> throw IllegalStateException("1234")}.static(entity_id)


        clientProtocol.bindStatic(client_entity, "top")
        serverProtocol.bindStatic(server_entity, "top")

        val task = client_entity.start(2)
        assertTrue { task.isFaulted }

        val taskResult = (task.result.valueOrThrow as RdTaskResult.Fault)
        assertEquals("1234", taskResult.error.reasonMessage)
        assertEquals("IllegalStateException", taskResult.error.reasonTypeFqn)

    }

    @Test
    fun testToString() {
        assertEquals("Success :: 1", RdTaskResult.Success(1).toString())
        assertEquals("Cancelled", RdTaskResult.Cancelled<Int>().toString())
        assertEquals("Fault :: com.jetbrains.rd.util.reactive.RdFault: error", RdTaskResult.Fault<Int>(Error("error")).toString())
    }
}

open class A(open val a:Any) {}

class B (override val a: String) : A(a) {}




