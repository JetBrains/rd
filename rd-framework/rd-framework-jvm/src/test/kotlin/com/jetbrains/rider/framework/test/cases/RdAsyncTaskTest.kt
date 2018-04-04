package com.jetbrains.rider.framework.test.cases

import com.jetbrains.rider.framework.*
import com.jetbrains.rider.framework.base.static
import com.jetbrains.rider.framework.impl.RdCall
import com.jetbrains.rider.framework.impl.RdEndpoint
import com.jetbrains.rider.framework.impl.RdOptionalProperty
import com.jetbrains.rider.framework.impl.RdTask
import com.jetbrains.rider.util.reactive.hasValue
import com.jetbrains.rider.util.reactive.valueOrThrow
import com.jetbrains.rider.util.threading.Linearization
import kotlin.concurrent.thread
import kotlin.test.*

class RdAsyncTaskTest : RdTestBase() {
    @Suppress("UNCHECKED_CAST")
    @Test
    fun TestDynamic() {
        val property_id = 1
        val client_property = RdOptionalProperty(RdCall as ISerializer<RdCall<Int, String>>).static(property_id)
        val server_property = RdOptionalProperty(RdEndpoint.Companion as ISerializer<RdEndpoint<Int, String>>).static(property_id).slave()


        //bound
        clientProtocol.bindStatic(client_property, "top")
        serverProtocol.bindStatic(server_property, "top")

        server_property.set ( RdEndpoint(Int::toString) )


        assertEquals("1", client_property.valueOrThrow.sync(1))

        val l = Linearization()
        server_property.set (
            RdEndpoint<Int, String> { _, v ->
                RdTask<String>().apply {
                    thread {
                        l.point(1)
                        this.set(v.toString())
                        l.point(2)
                    }
                }
            }
        )

        l.point(0)
        assertEquals("2", client_property.valueOrThrow.sync(2))
        l.point(3)
        l.reset()


        //wait for task
        val task = client_property.valueOrThrow.start(3)
        assertFalse(task.isSucceeded)
        assertFalse(task.isCanceled)
        assertFalse(task.isFaulted)
        assertFalse(task.result.hasValue)
        l.point(0)

        l.point(3)
        assertTrue(task.isSucceeded, "${task.result.hasValue}")
        assertFalse(task.isCanceled)
        assertFalse(task.isFaulted)
        assertTrue(task.result.hasValue)

        l.reset()
        val interruptedTask = client_property.valueOrThrow.start(0)
        //terminate lifetime by setting another endpoint
        server_property.set( RdEndpoint(Int::toString))
        assertTrue { interruptedTask.isCanceled }
        l.disable()

        when (interruptedTask.result.valueOrThrow) {
            is RdTaskResult.Success<*> -> assertFails {  }
            is RdTaskResult.Cancelled<*> -> {}
            is RdTaskResult.Fault<*> -> assertFails {  }
            else -> assertFails {  }
        }
    }

}


