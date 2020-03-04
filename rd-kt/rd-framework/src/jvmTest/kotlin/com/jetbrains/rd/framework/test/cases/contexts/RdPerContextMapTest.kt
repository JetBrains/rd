@file:Suppress("UNCHECKED_CAST")
package com.jetbrains.rd.framework.test.cases.contexts

import com.jetbrains.rd.framework.FrameworkMarshallers
import com.jetbrains.rd.framework.ISerializer
import com.jetbrains.rd.framework.RdContext
import com.jetbrains.rd.framework.RdId
import com.jetbrains.rd.framework.base.static
import com.jetbrains.rd.framework.impl.RdMap
import com.jetbrains.rd.framework.impl.RdPerContextMap
import com.jetbrains.rd.framework.test.util.DynamicEntity
import com.jetbrains.rd.framework.test.util.RdFrameworkTestBase
import com.jetbrains.rd.util.assert
import com.jetbrains.rd.util.lifetime.onTermination
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test


class RdPerContextMapTest : RdFrameworkTestBase() {
    companion object {
        @BeforeAll
        @AfterAll
        @JvmStatic
        fun resetContext() {
            ContextsTest.TestKeyHeavy.value = null
            ContextsTest.TestKeyLight.value = null
        }
    }

    @Test
    fun testOnStructMap() {
        val key = ContextsTest.TestKeyHeavy

        val serverMap = RdPerContextMap(key) { RdMap(FrameworkMarshallers.Int, FrameworkMarshallers.String).apply { master = it } }.static(1)
        val clientMap = RdPerContextMap(key) { RdMap(FrameworkMarshallers.Int, FrameworkMarshallers.String).apply { master = it } }.static(1).apply { master = false }

        val server1Cid = "Server-1"
        val client1Cid = "Client-1"

        serverProtocol.contexts.registerContext(key)
        clientProtocol.contexts.registerContext(key)

        serverProtocol.contexts.getValueSet(key).add(server1Cid)

        clientMap.bind(clientLifetime, clientProtocol, "map")
        serverMap.bind(serverLifetime, serverProtocol, "map")
        serverMap[server1Cid]!![1] = "test"

        assert(clientMap[server1Cid]!![1] == "test")

        clientProtocol.contexts.getValueSet(key).add(client1Cid)

        assert(serverMap[client1Cid] != null)
        assert(serverMap[client1Cid]!![1] == null)
    }

    @Test
    fun testOnDynamicMap() {
        val key = ContextsTest.TestKeyHeavy

        val serverMap = RdPerContextMap(key) { RdMap(FrameworkMarshallers.Int, DynamicEntity as ISerializer<DynamicEntity<String>>).apply { master = it } }.static(1)
        val clientMap = RdPerContextMap(key) { RdMap(FrameworkMarshallers.Int, DynamicEntity as ISerializer<DynamicEntity<String>>).apply { master = it } }.static(1).apply { master = false }

        val server1Cid = "Server-1"
        val client1Cid = "Client-1"

        serverProtocol.contexts.registerContext(key)
        clientProtocol.contexts.registerContext(key)

        serverProtocol.contexts.getValueSet(key).add(server1Cid)

        clientMap.bind(clientLifetime, clientProtocol, "map")
        serverMap.bind(serverLifetime, serverProtocol, "map")
        serverMap[server1Cid]!![1] = DynamicEntity("test")

        assert(clientMap[server1Cid]!![1]!!.foo.value == "test")

        clientProtocol.contexts.getValueSet(key).add(client1Cid)

        assert(serverMap[client1Cid] != null)
        assert(serverMap[client1Cid]!![1] == null)
    }

    @Test
    fun testLateBind01() {
        val key = ContextsTest.TestKeyHeavy

        val serverMap = RdPerContextMap(key) { RdMap(FrameworkMarshallers.Int, DynamicEntity as ISerializer<DynamicEntity<String>>).apply { master = it } }.static(1)
        val clientMap = RdPerContextMap(key) { RdMap(FrameworkMarshallers.Int, DynamicEntity as ISerializer<DynamicEntity<String>>).apply { master = it } }.static(1).apply { master = false }

        val server1Cid = "Server-1"
        val client1Cid = "Client-1"

        serverProtocol.contexts.registerContext(key)
        clientProtocol.contexts.registerContext(key)

        serverProtocol.contexts.getValueSet(key).add(server1Cid)

        serverMap[server1Cid]!![1] = DynamicEntity("test")
        clientProtocol.contexts.getValueSet(key).add(client1Cid)

        clientMap.bind(clientLifetime, clientProtocol, "map")
        serverMap.bind(serverLifetime, serverProtocol, "map")

        assert(clientMap[server1Cid]!![1]!!.foo.value == "test")

        assert(serverMap[client1Cid] != null)
        assert(serverMap[client1Cid]!![1] == null)
    }

    @Test
    fun testLateBind02() {
        val key = ContextsTest.TestKeyHeavy

        val serverMap = RdPerContextMap(key) { RdMap(FrameworkMarshallers.Int, DynamicEntity as ISerializer<DynamicEntity<String>>).apply { master = it } }.static(1)
        val clientMap = RdPerContextMap(key) { RdMap(FrameworkMarshallers.Int, DynamicEntity as ISerializer<DynamicEntity<String>>).apply { master = it } }.static(1).apply { master = false }

        val server1Cid = "Server-1"

        serverProtocol.contexts.registerContext(key)
        clientProtocol.contexts.registerContext(key)

        // no protocol value set value - pre-bind value will be lost

        serverMap[server1Cid]!![1] = DynamicEntity("test")

        clientMap.bind(clientLifetime, clientProtocol, "map")
        serverMap.bind(serverLifetime, serverProtocol, "map")

        assert(serverMap[server1Cid] == null)
        assert(clientMap[server1Cid] == null)
    }

    @Test
    fun testLateBind03() {
        val key = ContextsTest.TestKeyHeavy

        val serverMap = RdPerContextMap(key) { RdMap(FrameworkMarshallers.Int, DynamicEntity as ISerializer<DynamicEntity<String>>).apply { master = it } }.static(1)
        val clientMap = RdPerContextMap(key) { RdMap(FrameworkMarshallers.Int, DynamicEntity as ISerializer<DynamicEntity<String>>).apply { master = it } }.static(1).apply { master = false }

        val server1Cid = "Server-1"
        val server2Cid = "Server-2"

        serverProtocol.contexts.registerContext(key)
        clientProtocol.contexts.registerContext(key)

        serverProtocol.contexts.getValueSet(key).addAll(setOf(server1Cid, server2Cid))

        val log = ArrayList<String>()

        serverMap.view(serverLifetime) { entryLt, k, _ ->
            log.add("Add $k")
            entryLt.onTermination {
                log.add("Remove $k")
            }
        }

        serverMap[server1Cid]!![1] = DynamicEntity("test")
        serverMap[server2Cid]!![1] = DynamicEntity("test")

        clientMap.bind(clientLifetime, clientProtocol, "map")
        serverMap.bind(serverLifetime, serverProtocol, "map")

        assertEquals(listOf("Add $server1Cid", "Add $server2Cid"), log)
    }

    @Test
    fun testLateBind04() {
        val key = ContextsTest.TestKeyHeavy

        val serverMap = RdPerContextMap(key) { RdMap(FrameworkMarshallers.Int, DynamicEntity as ISerializer<DynamicEntity<String>>).apply { master = it } }.static(1)
        val clientMap = RdPerContextMap(key) { RdMap(FrameworkMarshallers.Int, DynamicEntity as ISerializer<DynamicEntity<String>>).apply { master = it } }.static(1).apply { master = false }

        val server1Cid = "Server-1"
        val server2Cid = "Server-2"

        serverProtocol.contexts.registerContext(key)
        clientProtocol.contexts.registerContext(key)

        serverProtocol.contexts.getValueSet(key).addAll(setOf(server1Cid))

        val log = ArrayList<String>()

        serverMap.view(serverLifetime) { entryLt, k, _ ->
            log.add("Add $k")
            entryLt.onTermination {
                log.add("Remove $k")
            }
        }

        serverMap[server1Cid]!![1] = DynamicEntity("test")
        serverMap[server2Cid]!![1] = DynamicEntity("test")

        clientMap.bind(clientLifetime, clientProtocol, "map")
        serverMap.bind(serverLifetime, serverProtocol, "map")

        assertEquals(listOf("Add $server1Cid", "Add $server2Cid", "Remove $server2Cid"), log)
    }

    @Test
    fun testLateBind05() {
        val key = ContextsTest.TestKeyHeavy

        val serverMap = RdPerContextMap(key) { RdMap(FrameworkMarshallers.Int, DynamicEntity as ISerializer<DynamicEntity<String>>).apply { master = it } }.static(1)
        val clientMap = RdPerContextMap(key) { RdMap(FrameworkMarshallers.Int, DynamicEntity as ISerializer<DynamicEntity<String>>).apply { master = it } }.static(1).apply { master = false }

        val server1Cid = "Server-1"

        serverProtocol.contexts.registerContext(key)
        clientProtocol.contexts.registerContext(key)

        val log = ArrayList<String>()

        serverMap.view(serverLifetime) { entryLt, k, _ ->
            log.add("Add $k")
            entryLt.onTermination {
                log.add("Remove $k")
            }
        }

        serverProtocol.contexts.getValueSet(key).addAll(setOf(server1Cid))

        clientMap.bind(clientLifetime, clientProtocol, "map")
        serverMap.bind(serverLifetime, serverProtocol, "map")

        serverMap[server1Cid]!![1] = DynamicEntity("test")

        assertEquals(listOf("Add $server1Cid"), log)
    }

    @Test
    fun testLateBind06() {
        val key = ContextsTest.TestKeyHeavy

        val serverMap = RdPerContextMap(key) { RdMap(FrameworkMarshallers.Int, DynamicEntity as ISerializer<DynamicEntity<String>>).apply { master = it } }.static(1)
        val clientMap = RdPerContextMap(key) { RdMap(FrameworkMarshallers.Int, DynamicEntity as ISerializer<DynamicEntity<String>>).apply { master = it } }.static(1).apply { master = false }

        val server1Cid = "Server-1"

        val log = ArrayList<String>()

        serverMap.view(serverLifetime) { entryLt, k, _ ->
            log.add("Add $k")
            entryLt.onTermination {
                log.add("Remove $k")
            }
        }

        serverProtocol.contexts.registerContext(key)
        clientProtocol.contexts.registerContext(key)

        clientMap.bind(clientLifetime, clientProtocol, "map")
        serverMap.bind(serverLifetime, serverProtocol, "map")

        key.value = server1Cid
        serverProtocol.wire.send(RdId.Null.mix(10)) {} // trigger key addition by protocol write
        key.value = null

        assertTrue(serverProtocol.contexts.getValueSet(key).contains(server1Cid))

        serverMap[server1Cid]!![1] = DynamicEntity("test")

        assertEquals(listOf("Add $server1Cid"), log)
    }

    @Test
    fun testValueSetChangesInContext() {
        val key1 = object : RdContext<String>("test-key1", true, FrameworkMarshallers.String) {}
        val key2 = object : RdContext<String>("test-key2", true, FrameworkMarshallers.String) {}

        val serverMap = RdPerContextMap(key1) { RdMap(FrameworkMarshallers.Int, DynamicEntity as ISerializer<DynamicEntity<String>>).apply { master = it } }.static(1)
        val clientMap = RdPerContextMap(key1) { RdMap(FrameworkMarshallers.Int, DynamicEntity as ISerializer<DynamicEntity<String>>).apply { master = it } }.static(1).apply { master = false }

        val server1Cid = "Server-1"
        val server2Cid = "Server-2"
        val server3Cid = "Server-3"
        val server4Cid = "Server-4"

        val log = ArrayList<String>()

        serverMap.view(serverLifetime) { entryLt, k, _ ->
            log.add("Add $k")
            entryLt.onTermination {
                log.add("Remove $k")
            }
        }

        key1.value = server1Cid
        key2.value = server1Cid

        serverProtocol.contexts.registerContext(key1)
        serverProtocol.contexts.registerContext(key2)
        clientProtocol.contexts.registerContext(key1)

        clientMap.bind(clientLifetime, clientProtocol, "map")
        serverMap.bind(serverLifetime, serverProtocol, "map")

        serverProtocol.contexts.getValueSet(key1).add(server2Cid)
        key1.value = server4Cid
        serverProtocol.contexts.getValueSet(key1).add(server3Cid)
        key1.value = null
        key2.value = null

        assertFalse(serverProtocol.contexts.getValueSet(key1).contains(server1Cid))
        assertFalse(serverProtocol.contexts.getValueSet(key2).contains(server1Cid))
        assertEquals(listOf("Add $server2Cid", "Add $server3Cid"), log)
    }
}
