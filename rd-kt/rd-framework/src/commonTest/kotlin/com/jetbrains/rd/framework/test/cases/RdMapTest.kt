package com.jetbrains.rd.framework.test.cases

import com.jetbrains.rd.framework.base.static
import com.jetbrains.rd.framework.impl.RdMap
import com.jetbrains.rd.framework.test.util.DynamicEntity
import com.jetbrains.rd.framework.test.util.RdFrameworkTestBase
import com.jetbrains.rd.util.lifetime.Lifetime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RdMapTest : RdFrameworkTestBase() {
    @Test
    fun testStatic() {
        val id = 1

        val serverMap = RdMap<Int, String>().static(id).apply { optimizeNested=true }
        val clientMap = RdMap<Int, String>().static(id).apply { optimizeNested=true }

        val logUpdate = arrayListOf<String>()
        clientMap.advise(Lifetime.Eternal) { entry -> logUpdate.add(entry.toString())}

        assertEquals(0, serverMap.count())
        assertEquals(0, clientMap.count())

        serverMap[1] = "Server value 1"
        serverMap[2] = "Server value 2"
        serverMap[3] = "Server value 3"

        assertEquals(0, clientMap.count())
        clientProtocol.bindStatic(clientMap, "top")
        serverProtocol.bindStatic(serverMap, "top")

        assertEquals(3, clientMap.count())
        assertEquals("Server value 1", clientMap[1])
        assertEquals("Server value 2", clientMap[2])
        assertEquals("Server value 3", clientMap[3])

        serverMap[4] = "Server value 4"
        clientMap[4] = "Client value 4"

        assertEquals("Client value 4", clientMap[4])
        assertEquals("Client value 4", serverMap[4])

        clientMap[5] = "Client value 5"
        serverMap[5] = "Server value 5"


        assertEquals("Server value 5", clientMap[5])
        assertEquals("Server value 5", serverMap[5])


        assertEquals(listOf("Add 1:Server value 1",
                "Add 2:Server value 2",
                "Add 3:Server value 3",
                "Add 4:Server value 4",
                "Update 4:Client value 4",
                "Add 5:Client value 5",
                "Update 5:Server value 5"),
            logUpdate
        )
    }

    @Test
    fun testDynamic() {
        val id = 1

        val serverMap = RdMap<Int, DynamicEntity<Boolean?>>().static(id)
        val clientMap = RdMap<Int, DynamicEntity<Boolean?>>().static(id)
        serverMap.master = false

        DynamicEntity.create(clientProtocol)
        DynamicEntity.create(serverProtocol)

        assertTrue(serverMap.count() == 0)
        assertTrue(clientMap.count() == 0)

        clientProtocol.bindStatic(clientMap, "top")
        serverProtocol.bindStatic(serverMap," top")

        val log = arrayListOf<String>()
        serverMap.view(Lifetime.Eternal) { lf, k, v ->
            lf.bracket({log.add("start $k")}, {log.add("finish $k")})
            v.foo.advise(lf) { fooval -> log.add("$fooval")}
        }
        clientMap[1] = DynamicEntity<Boolean?>(null)
        clientMap[1]!!.foo.value = true
        clientMap[1]!!.foo.value = true //no action

        clientMap[1] = DynamicEntity<Boolean?>(true)

        serverMap[2] = DynamicEntity<Boolean?>(false)

        clientMap.remove(2)
        clientMap[2] = DynamicEntity<Boolean?>(true)

        clientMap.clear()

        assertEquals(listOf("start 1", "null", "true",
                "finish 1", "start 1", "true",
                "start 2", "false",
                "finish 2", "start 2", "true",
                "finish 1", "finish 2"), log)
    }


}