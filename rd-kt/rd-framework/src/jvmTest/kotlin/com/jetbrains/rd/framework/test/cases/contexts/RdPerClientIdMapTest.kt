@file:Suppress("UNCHECKED_CAST")
package com.jetbrains.rd.framework.test.cases.contexts

import com.jetbrains.rd.framework.ClientId
import com.jetbrains.rd.framework.FrameworkMarshallers
import com.jetbrains.rd.framework.ISerializer
import com.jetbrains.rd.framework.RdContextKey
import com.jetbrains.rd.framework.base.static
import com.jetbrains.rd.framework.impl.RdMap
import com.jetbrains.rd.framework.impl.RdPerContextMap
import com.jetbrains.rd.framework.test.util.DynamicEntity
import com.jetbrains.rd.framework.test.util.RdFrameworkTestBase
import com.jetbrains.rd.util.assert
import org.junit.Test

class RdPerClientIdMapTest : RdFrameworkTestBase() {
    @Test
    fun testOnStructMap() {
        val key = RdContextKey<String>("test-key", true, FrameworkMarshallers.String)

        val serverMap = RdPerContextMap(key) { RdMap(FrameworkMarshallers.Int, FrameworkMarshallers.String).apply { master = it } }.static(1)
        val clientMap = RdPerContextMap(key) { RdMap(FrameworkMarshallers.Int, FrameworkMarshallers.String).apply { master = it } }.static(1).apply { master = false }

        val server1Cid = ClientId("Server-1")
        val client1Cid = ClientId("Client-1")

        serverProtocol.contextHandler.registerKey(key)
        clientProtocol.contextHandler.registerKey(key)

        serverProtocol.contextHandler.getValueSet(key).add(server1Cid.value)

        clientMap.bind(clientLifetime, clientProtocol, "map")
        serverMap.bind(serverLifetime, serverProtocol, "map")
        serverMap[server1Cid.value]!![1] = "test"

        assert(clientMap[server1Cid.value]!![1] == "test")

        clientProtocol.contextHandler.getValueSet(key).add(client1Cid.value)

        assert(serverMap[client1Cid.value] != null)
        assert(serverMap[client1Cid.value]!![1] == null)
    }

    @Test
    fun testOnDynamicMap() {
        val key = RdContextKey<String>("test-key", true, FrameworkMarshallers.String)

        val serverMap = RdPerContextMap(key) { RdMap(FrameworkMarshallers.Int, DynamicEntity as ISerializer<DynamicEntity<String>>).apply { master = it } }.static(1)
        val clientMap = RdPerContextMap(key) { RdMap(FrameworkMarshallers.Int, DynamicEntity as ISerializer<DynamicEntity<String>>).apply { master = it } }.static(1).apply { master = false }

        val server1Cid = ClientId("Server-1")
        val client1Cid = ClientId("Client-1")

        serverProtocol.contextHandler.registerKey(key)
        clientProtocol.contextHandler.registerKey(key)

        serverProtocol.contextHandler.getValueSet(key).add(server1Cid.value)

        clientMap.bind(clientLifetime, clientProtocol, "map")
        serverMap.bind(serverLifetime, serverProtocol, "map")
        serverMap[server1Cid.value]!![1] = DynamicEntity("test")

        assert(clientMap[server1Cid.value]!![1]!!.foo.value == "test")

        clientProtocol.contextHandler.getValueSet(key).add(client1Cid.value)

        assert(serverMap[client1Cid.value] != null)
        assert(serverMap[client1Cid.value]!![1] == null)
    }
}
