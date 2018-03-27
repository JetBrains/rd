package com.jetbrains.rider.util.test.cases
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.lifetime.plusAssign
import com.jetbrains.rider.util.reactive.IMutableViewableMap
import com.jetbrains.rider.util.reactive.ViewableMap
import kotlin.test.Test
import kotlin.test.assertEquals

class ViewableMapTest {
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
            map.adviseAddRemove(lifetime, {kind, key, value -> logAddRemove.add("${kind} ${key}:${value}")} )
            map.advise(lifetime, {entry -> logUpdate.add(entry.toString())} )
            map.view(lifetime, {inner, x -> inner.bracket({ logView.add(x.key) }, { logView.add(-x.value) }) })

            lifetime += {logAddRemove.add("End")}

            map[0] = 1
            map[10] = 10
            map[0] = 0
            map.remove(1)
        }

        assertEquals(listOf("Add 0:0", "Add 1:1", "Remove 0:0", "Add 0:1", "Add 10:10", "Remove 0:1", "Add 0:0", "Remove 1:1", "End"), logAddRemove)
        assertEquals(listOf("Add 0:0", "Add 1:1", "Update 0:1", "Add 10:10", "Update 0:0", "Remove 1:null"), logUpdate)
        assertEquals(listOf(0, 1, -0, 0, 10, -1, 0, -1, /*this events are arguable*/0, -10), logView);

        //let's clear
        logAddRemove.clear()
        Lifetime.using { lifetime ->
            map.adviseAddRemove(lifetime, { kind, key, value ->  logAddRemove.add("${kind} ${key}:${value}") })
            map[0] = 0 //same shit, but we need to fire event I think
            map.clear()
        }
        assertEquals(listOf("Add 0:0", "Add 10:10", "Remove 0:0", "Remove 10:10"), logAddRemove)
    }
}


