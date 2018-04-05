package com.jetbrains.rider.framework.test.cases

import com.jetbrains.rider.framework.base.static
import com.jetbrains.rider.framework.impl.RdSet
import com.jetbrains.rider.util.reactive.AddRemove
import kotlin.test.Test
import kotlin.test.assertEquals


class RdSetTest : RdTestBase() {
    @Test
    fun testStatic() {
        val id = 1
        val serverSet = RdSet<Int>().static(id)
        val clientSet = RdSet<Int>().static(id)

        val log = arrayListOf<Int>()
        serverSet.advise(serverLifetimeDef.lifetime) {kind, v -> log.add(if (kind == AddRemove.Add) v else -v)}

        clientSet.add(1)
        clientSet.add(1)
        clientSet.add(2)
        clientSet.add(3)


        assertEquals(listOf<Int>(), log)

        serverProtocol.bindStatic(serverSet, "top")
        clientProtocol.bindStatic(clientSet, "top")
        assertEquals(listOf(1, 2, 3), log)

        clientSet.retainAll(listOf(1, 2))
        assertEquals(listOf(1, 2, 3, -3), log)

        serverSet.remove(3)
        clientSet.remove(3)
        assertEquals(listOf(1, 2, 3, -3), log)

        clientSet.removeAll { it == 1 }
        assertEquals(listOf(1, 2, 3, -3, -1), log)


        clientSet.clear()
        assertEquals(listOf(1, 2, 3, -3, -1, -2), log)
    }
}