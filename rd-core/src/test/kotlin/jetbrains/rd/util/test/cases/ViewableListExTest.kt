package com.jetbrains.rider.util.test.cases

import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.reactive.ViewableList
import com.jetbrains.rider.util.reactive.flowInto
import com.jetbrains.rider.util.reactive.flowIntoSorted
import org.testng.Assert
import org.testng.annotations.Test
import java.util.*

class ViewableListExTest {
    @Test
    fun flowInto() {
        val list = ViewableList<Int>()
        val list2 = ViewableList<Int>()
        list.add(0)
        list.add(1)
        val logList2 = arrayListOf<String>()
        Lifetime.using { lifetime ->
            list2.advise(lifetime) { args -> logList2.add("${args.kind.name} ${args.value}") }
            list.flowInto(lifetime, list2)

            Assert.assertEquals(logList2, arrayListOf("Add 0", "Add 1"), "attach flowInto")
            Assert.assertEquals(list, arrayListOf(0, 1), "attach flowInto")
            logList2.clear()

            list.remove(0)
            Assert.assertEquals(logList2, arrayListOf("Remove 0"), "remove flowInto")
            Assert.assertEquals(list, arrayListOf(1), "remove flowInto")
            logList2.clear()

            list.add(2)
            Assert.assertEquals(logList2, arrayListOf("Add 2"), "add flowInto")
            Assert.assertEquals(list, arrayListOf(1, 2), "add flowInto")
            logList2.clear()
        }
    }

    @Test
    fun flowIntoSorted1() {
        val list = ViewableList<Int>()
        val list2 = ViewableList<Int>()
        list.add(0)
        list.add(1)
        Lifetime.using { lt ->
            list.flowIntoSorted(lt, list2, java.util.Comparator.comparingInt({it}), { true })
            Assert.assertEquals(list2, listOf(0, 1))

            list.remove(0)
            Assert.assertEquals(list2, listOf(1))

            list.addAll(listOf(2, 3, 7, -1))
            list.add(4)
            Assert.assertEquals(list2, listOf(-1, 1, 2, 3 ,4, 7))
        }
    }
    @Test
    fun flowIntoSorted2() {
        val list = ViewableList<Int>()
        val list2 = ViewableList<Int>()
        list.add(0)
        list.add(1)
        list2.addAll(listOf(2, 3, 4))
        Lifetime.using { lt ->
            list.flowIntoSorted(lt, list2, java.util.Comparator.comparingInt({it}), { true })
            Assert.assertEquals(list2, listOf(0, 1))
        }
        Assert.assertEquals(list2, listOf<Int>())
    }

    @Test
    fun flowIntoSorted3() {
        val list = ViewableList<Int>()
        val list2 = ViewableList<Int>()
        list.add(0)
        list.add(1)
        Lifetime.using { lt ->
            list.flowIntoSorted(lt, list2, Comparator { x, y -> 0 }, { true })
            Assert.assertEquals(list2, listOf(0, 1))
        }
    }
}