package com.jetbrains.rd.framework.test.cases

import com.jetbrains.rd.framework.ISerializer
import com.jetbrains.rd.framework.base.static
import com.jetbrains.rd.framework.impl.RdList
import com.jetbrains.rd.framework.impl.RdProperty
import com.jetbrains.rd.framework.test.util.DynamicEntity
import com.jetbrains.rd.framework.test.util.RdFrameworkTestBase
import com.jetbrains.rd.util.lifetime.Lifetime
import kotlin.test.Test
import kotlin.test.assertEquals

class RdListTest : RdFrameworkTestBase() {
    @Test
    fun testStatic() {
        val id = 1

        val serverList = RdList<String>().static(id).apply { optimizeNested = true }
        val clientList = RdList<String>().static(id).apply { optimizeNested = true }

        val logUpdate = arrayListOf<String>()
        clientList.advise(Lifetime.Eternal) { entry -> logUpdate.add(entry.toString()) }

        assertEquals(0, serverList.count())
        assertEquals(0, clientList.count())

        serverList.add("Server value 1")
        serverList.addAll(listOf("Server value 2", "Server value 3"))

        assertEquals(0, clientList.count())
        clientProtocol.bindStatic(clientList, "top")
        serverProtocol.bindStatic(serverList, "top")

        assertEquals(clientList.count(), 3)
        assertEquals(clientList[0], "Server value 1")
        assertEquals(clientList[1], "Server value 2")
        assertEquals(clientList[2], "Server value 3")

        serverList.add("Server value 4")
        clientList[3] = "Client value 4"

        assertEquals(clientList[3], "Client value 4")
        assertEquals(serverList[3], "Client value 4")

        clientList.add("Client value 5")
        serverList[4] = "Server value 5"


        assertEquals(clientList[4], "Server value 5")
        assertEquals(serverList[4], "Server value 5")


        assertEquals(logUpdate,
                listOf("Add 0:Server value 1",
                        "Add 1:Server value 2",
                        "Add 2:Server value 3",
                        "Add 3:Server value 4",
                        "Update 3:Client value 4",
                        "Add 4:Client value 5",
                        "Update 4:Server value 5")
        )
    }

    @Test
    fun testDynamic() {
        val id = 1

        val serverList = RdList<DynamicEntity<Boolean?>>().static(id)
        val clientList = RdList<DynamicEntity<Boolean?>>().static(id)

        DynamicEntity.create(clientProtocol)
        DynamicEntity.create(serverProtocol)

        assertEquals(0, serverList.count())
        assertEquals(0, clientList.count())

        clientProtocol.bindStatic(clientList, "top")
        serverProtocol.bindStatic(serverList, " top")

        val log = arrayListOf<String>()
        serverList.view(Lifetime.Eternal) { lf, k, v ->
            lf.bracket({ log.add("start $k") }, { log.add("finish $k") })
            v.foo.advise(lf) { fooval -> log.add("$fooval") }
        }
        clientList.add(DynamicEntity<Boolean?>(null))
        clientList[0].foo.value = true
        clientList[0].foo.value = true //no action

        clientList[0] = DynamicEntity(true)

        serverList.add(DynamicEntity(false))

        clientList.removeAt(1)
        clientList.add(DynamicEntity(true))

        clientList.clear()

        assertEquals(log, listOf("start 0", "null", "true",
                "finish 0", "start 0", "true",
                "start 1", "false",
                "finish 1", "start 1", "true",
                "finish 1", "finish 0"))
    }


    @Suppress("UNCHECKED_CAST")
    @Test
    fun testOfProperties() {
        val id = 1

        val szr = RdProperty.Companion as ISerializer<RdProperty<Int>>

        val serverList = RdList(szr).static(id)
        val clientList = RdList(szr).static(id)

        assertEquals(0, serverList.count())
        assertEquals(0, clientList.count())

        assertEquals(0, clientList.count())
        clientProtocol.bindStatic(clientList, "top")
        serverProtocol.bindStatic(serverList, "top")

        val log = arrayListOf<String>()

        serverList.view(Lifetime.Eternal) { lf, k, v ->
            lf.bracket(
                    { log.add("start $k") },
                    { log.add("finish $k") }
            )
            v.advise(lf) { fooval -> log.add(fooval.toString()) }
        }


        serverList.add(RdProperty(0))

        clientList.add(RdProperty(0))

        clientList[0] = RdProperty(2)

        serverList.add(RdProperty(1))

        serverList.add(RdProperty(8))

        clientList.clear()

        assertEquals(listOf(
                "start 0", "0",
                "start 1", "0",
                "finish 0", "start 0", "2",
                "start 2", "1",
                "start 3", "8",
                "finish 3",
                "finish 2",
                "finish 1",
                "finish 0"
        ), log)
    }
}

