package com.jetbrains.rd.framework.test.cases

import com.jetbrains.rd.framework.base.static
import com.jetbrains.rd.framework.impl.RdSet
import com.jetbrains.rd.framework.test.util.RdFrameworkTestBase
import com.jetbrains.rd.util.reactive.AddRemove
import kotlin.test.Test
import kotlin.test.assertEquals


class RdSetTest : RdFrameworkTestBase() {
    @Test
    fun testStatic() {
        val id = 1
        val serverSet = RdSet<Int>().static(id)
        val clientSet = RdSet<Int>().static(id)

        val log = arrayListOf<Int>()
        serverSet.advise(serverLifetimeDef.lifetime) { kind, v -> log.add(if (kind == AddRemove.Add) v else -v) }

        clientSet.add(2)
        clientSet.add(0)
        clientSet.add(1)
        clientSet.add(8)
        clientSet.add(3)


        assertEquals(listOf<Int>(), log)

        serverProtocol.bindStatic(serverSet, "top")
        clientProtocol.bindStatic(clientSet, "top")
        assertEquals(listOf(2, 0, 1, 8, 3), log)

        clientSet.retainAll(listOf(8, 3, 0))
        assertEquals(listOf(2, 0, 1, 8, 3, -2, -1), log)

        serverSet.remove(1)
        clientSet.remove(1)
        assertEquals(listOf(2, 0, 1, 8, 3, -2, -1), log)

        clientSet.removeAll { it == 3 }
        assertEquals(listOf(2, 0, 1, 8, 3, -2, -1, -3), log)


        clientSet.clear()
        assertEquals(listOf(2, 0, 1, 8, 3, -2, -1, -3, -0, -8), log)
    }
}