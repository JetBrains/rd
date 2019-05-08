package com.jetbrains.rd.util.test.cases
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.plusAssign
import com.jetbrains.rd.util.reactive.IMutableViewableMap
import com.jetbrains.rd.util.reactive.ViewableMap
import com.jetbrains.rd.util.test.framework.RdTestBase
import kotlin.test.Test
import kotlin.test.assertEquals

class ViewableMapTest : RdTestBase()  {
    @Test
    fun testAdvise() {

        val map : IMutableViewableMap<Int, Int> = ViewableMap()
        map[0] = 1
        map[1] = 1
        map[2] = 2
        map[0] = 0
        map.remove(2)

        val logAddRemove = arrayListOf<String>()
        val logUpdate = arrayListOf<String>()
        val logView = arrayListOf<Int>()
        Lifetime.using { lifetime ->
            map.adviseAddRemove(lifetime) { kind, key, value -> logAddRemove.add("${kind} ${key}:${value}")}
            map.advise(lifetime) { entry -> logUpdate.add(entry.toString())}
            map.view(lifetime) { inner, x -> inner.bracket({ logView.add(x.key) }, { logView.add(-x.value) }) }

            lifetime += {logAddRemove.add("End")}

            map[0] = 1
            map[10] = 10
            map[0] = 0
            map.remove(1)
        }

        assertEquals(listOf("Add 0:0", "Add 1:1", "Remove 0:0", "Add 0:1", "Add 10:10", "Remove 0:1", "Add 0:0", "Remove 1:1", "End"), logAddRemove)
        assertEquals(listOf("Add 0:0", "Add 1:1", "Update 0:1", "Add 10:10", "Update 0:0", "Remove 1"), logUpdate)
        assertEquals(listOf(0, 1, -0, 0, 10, -1, 0, -1, /*this events are arguable*/0, -10), logView)

        //let's clear
        logAddRemove.clear()
        Lifetime.using { lifetime ->
            map.adviseAddRemove(lifetime) { kind, key, value ->  logAddRemove.add("${kind} ${key}:${value}") }
            map[0] = 0 //same shit, but we need to fire event I think
            map.clear()
        }
        assertEquals(listOf("Add 0:0", "Add 10:10", "Remove 0:0", "Remove 10:10"), logAddRemove)
    }

    @Test
    fun testView() {
        val elementsView = listOf(2, 0, 1, 8, 3)
        val elementsUnView = listOf(1, 3, 8, 0, 2)

        val indexesUnView = listOf(2, 4, 3, 1, 0)

        val map: IMutableViewableMap<Int, Int> = ViewableMap()
        val log = arrayListOf<String>()
        Lifetime.using { lifetime ->
            map.view(lifetime) { lt, value ->
                log.add("View (${value.key}, ${value.value})")
                lt += { log.add("UnView (${value.key}, ${value.value})") } }
            elementsView.forEachIndexed { index, v -> map.set(index, v) }
            map.remove(2)
        }
        val a = elementsView.mapIndexed{ index, value -> "View ($index, $value)" }
        val b = indexesUnView.zip(elementsUnView).map { "UnView (${it.first}, ${it.second})" }
        assertEquals(a.union(b).toList(), log)
    }

}


