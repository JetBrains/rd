package com.jetbrains.rd.framework.test.cases

import com.jetbrains.rd.framework.ISerializer
import com.jetbrains.rd.framework.base.static
import com.jetbrains.rd.framework.impl.RdOptionalProperty
import com.jetbrains.rd.framework.impl.RdProperty
import com.jetbrains.rd.framework.test.util.DynamicEntity
import com.jetbrains.rd.framework.test.util.RdFrameworkTestBase
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.valueOrThrow
import com.jetbrains.rd.util.string.printToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RdPropertyTest : RdFrameworkTestBase() {
    @Test
    fun testStatic() {
        val property_id = 1

        val client_property = RdProperty(1).static(property_id)
        val server_property = RdProperty(1).static(property_id).slave()

        val clientLog = arrayListOf<Int>()
        val serverLog = arrayListOf<Int>()
        client_property.advise(Lifetime.Eternal) { clientLog.add(it) }
        server_property.advise(Lifetime.Eternal) { serverLog.add(it) }

        //not bound
        assertEquals(listOf(1), clientLog)
//        assertEquals(listOf(1), serverLog)

//        assertFails { client_property.value = 2 } do not count NotBound any more
//        assertFails { server_property.value = 2 }
        assertEquals(listOf(1), clientLog)
//        assertEquals(listOf(1), serverLog)

        //bound
        serverProtocol.bindStatic(server_property, "top")
        clientProtocol.bindStatic(client_property, "top")

        assertEquals(listOf(1), clientLog)
        assertEquals(listOf(1), serverLog)

        //set from client
        client_property.value = 2
        assertEquals(listOf(1, 2), clientLog)
        assertEquals(listOf(1, 2), serverLog)

        //set from server
        server_property.value = 3
        assertEquals(listOf(1, 2, 3), clientLog)
        assertEquals(listOf(1, 2, 3), serverLog)
    }


    @Test
    fun testDynamic() {
        val property_id = 1
        val client_property = RdOptionalProperty<DynamicEntity<Boolean?>>().static(property_id)
        val server_property = RdOptionalProperty<DynamicEntity<Boolean?>>().static(property_id).slave()

        DynamicEntity.create(clientProtocol)
        DynamicEntity.create(serverProtocol)
        //bound
        clientProtocol.bindStatic(client_property, "top")
        serverProtocol.bindStatic(server_property, "top")

        val clientLog = arrayListOf<Boolean?>()
        val serverLog = arrayListOf<Boolean?>()

        client_property.advise(Lifetime.Eternal) { entity -> entity.foo.advise(Lifetime.Eternal) { clientLog.add(it) } }
        server_property.advise(Lifetime.Eternal) { entity -> entity.foo.advise(Lifetime.Eternal) { serverLog.add(it) } }

        assertEquals(listOf<Boolean?>(), clientLog)
        assertEquals(listOf<Boolean?>(), serverLog)

        client_property.set(DynamicEntity<Boolean?>(null))

        assertEquals(listOf<Boolean?>(null), clientLog)
        assertEquals(listOf<Boolean?>(null), serverLog)


        client_property.valueOrThrow.foo.set(false)
        assertNotNull(client_property.printToString())
        assertEquals(listOf(null, false), clientLog)
        assertEquals(listOf(null, false), serverLog)

        server_property.valueOrThrow.foo.set(true)
        assertEquals(listOf(null, false, true), clientLog)
        assertEquals(listOf(null, false, true), serverLog)

        server_property.valueOrThrow.foo.set(null)
        assertEquals(listOf(null, false, true, null), clientLog)
        assertEquals(listOf(null, false, true, null), serverLog)


        val e = DynamicEntity<Boolean?>(true)
        client_property.set(e) //no listen - no change
        assertEquals(listOf(null, false, true, null, true), clientLog)
        assertEquals(listOf(null, false, true, null, true), serverLog)

        server_property.valueOrThrow.foo.set(null)
        assertEquals(listOf(null, false, true, null, true, null), clientLog)
        assertEquals(listOf(null, false, true, null, true, null), serverLog)


        client_property.set(DynamicEntity(false))
        //reuse
        client_property.set(e)

    }

    @Test
    fun testDynamic2() {
        val property_id = 1
        val client_property = RdProperty(DynamicEntity(RdProperty(0).static(3)).static(2)).static(property_id)
        val server_property = RdProperty(DynamicEntity(RdProperty(0).static(3)).static(2)).static(property_id).slave()

        DynamicEntity.create(clientProtocol)
        DynamicEntity.create(serverProtocol)
        //bound
        clientProtocol.bindStatic(client_property, "top")
        serverProtocol.bindStatic(server_property, "top")

        val clientLog = arrayListOf<Int>()
        val serverLog = arrayListOf<Int>()

        client_property.advise(Lifetime.Eternal) { entity -> entity.foo.advise(Lifetime.Eternal, { clientLog.add(it) }) }
        server_property.advise(Lifetime.Eternal) { entity -> entity.foo.advise(Lifetime.Eternal, { serverLog.add(it) }) }

        assertEquals(arrayListOf(0), clientLog)
        assertEquals(arrayListOf(0), serverLog)

        client_property.set(DynamicEntity(2))

        assertEquals((arrayListOf(0, 2)), clientLog)
        assertEquals((arrayListOf(0, 2)), serverLog)

        client_property.value.foo.set(5)

        assertEquals(arrayListOf(0, 2, 5), clientLog)
        assertEquals((arrayListOf(0, 2, 5)), serverLog)

        client_property.value.foo.set(5)

        assertEquals(arrayListOf(0, 2, 5), clientLog)
        assertEquals(arrayListOf(0, 2, 5), serverLog)

        client_property.set(DynamicEntity(5))

        assertEquals(arrayListOf(0, 2, 5, 5), clientLog)
        assertEquals(arrayListOf(0, 2, 5, 5), serverLog)
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun testEarlyAdvise() {
        setWireAutoFlush(false)

        val szr = RdOptionalProperty.Companion as ISerializer<RdOptionalProperty<Int>>
        val p1 = RdOptionalProperty(szr)
        val p2 = RdOptionalProperty(szr)

        var nxt = 0
        val log = arrayListOf<Int>()
        p1.view(clientLifetimeDef.lifetime) { lf, inner -> inner.advise(lf) { log.add(it) } }
        p2.advise(serverLifetimeDef.lifetime) { inner -> inner.set(++nxt) }

        clientProtocol.bindStatic(p1, 1)
        serverProtocol.bindStatic(p2, 1)
        p1.set(RdOptionalProperty())

        setWireAutoFlush(true)
        assertEquals(listOf(1), log)


    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun testCompanion() {
        val p1 = RdProperty(RdProperty(0).static(2), RdProperty.Companion as ISerializer<RdProperty<Int>>)
        val p2 = RdProperty(RdProperty(0).static(2), RdProperty.Companion as ISerializer<RdProperty<Int>>)


        var nxt = 10
        val log = arrayListOf<Int>()
        p1.view(clientLifetimeDef.lifetime) { lf, inner ->
            inner.advise(lf) { it -> log.add(it); }
        }
        p2.advise(serverLifetimeDef.lifetime) { inner -> inner.set(++nxt); }

        clientProtocol.bindStatic(p1, 1)
        serverProtocol.bindStatic(p2, 1)

        p2.set(RdProperty(0))

        assertEquals(arrayListOf(0, 0, 12), log)
    }
}


