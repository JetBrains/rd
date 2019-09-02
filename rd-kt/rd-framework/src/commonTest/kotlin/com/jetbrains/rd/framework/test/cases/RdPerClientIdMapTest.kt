package com.jetbrains.rd.framework.test.cases

import com.jetbrains.rd.framework.ClientId
import com.jetbrains.rd.framework.FrameworkMarshallers
import com.jetbrains.rd.framework.ISerializer
import com.jetbrains.rd.framework.base.static
import com.jetbrains.rd.framework.impl.RdMap
import com.jetbrains.rd.framework.impl.RdPerClientIdMap
import com.jetbrains.rd.framework.test.util.DynamicEntity
import com.jetbrains.rd.framework.test.util.RdFrameworkTestBase
import com.jetbrains.rd.util.assert
import kotlin.test.Test

class RdPerClientIdMapTest : RdFrameworkTestBase() {
    @Test
    fun testOnStructMap() {
        val serverMap = RdPerClientIdMap { RdMap(FrameworkMarshallers.Int, FrameworkMarshallers.String).apply { master = it } }.static(1)
        val clientMap = RdPerClientIdMap { RdMap(FrameworkMarshallers.Int, FrameworkMarshallers.String).apply { master = it } }.static(1).apply { master = false }

        val server1Cid = ClientId("Server-1")
        val client1Cid = ClientId("Client-1")

        serverProtocol.clientIdSet.add(server1Cid)

        clientMap.bind(clientLifetime, clientProtocol, "map")
        serverMap.bind(serverLifetime, serverProtocol, "map")
        serverMap[server1Cid]!![1] = "test"

        assert(clientMap[server1Cid]!![1] == "test")

        clientProtocol.clientIdSet.add(client1Cid)

        assert(serverMap[client1Cid] != null)
        assert(serverMap[client1Cid]!![1] == null)
    }

    @Test
    fun testOnDynamicMap() {
        val serverMap = RdPerClientIdMap { RdMap(FrameworkMarshallers.Int, DynamicEntity as ISerializer<DynamicEntity<String>>).apply { master = it } }.static(1)
        val clientMap = RdPerClientIdMap { RdMap(FrameworkMarshallers.Int, DynamicEntity as ISerializer<DynamicEntity<String>>).apply { master = it } }.static(1).apply { master = false }

        val server1Cid = ClientId("Server-1")
        val client1Cid = ClientId("Client-1")

        serverProtocol.clientIdSet.add(server1Cid)

        clientMap.bind(clientLifetime, clientProtocol, "map")
        serverMap.bind(serverLifetime, serverProtocol, "map")
        serverMap[server1Cid]!![1] = DynamicEntity("test")

        assert(clientMap[server1Cid]!![1]!!.foo.value == "test")

        clientProtocol.clientIdSet.add(client1Cid)

        assert(serverMap[client1Cid] != null)
        assert(serverMap[client1Cid]!![1] == null)
    }
}