package com.jetbrains.rider.util.test.cases

import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.lifetime.plusAssign
import com.jetbrains.rider.util.reactive.IMutableViewableList
import com.jetbrains.rider.util.reactive.ViewableList
import org.testng.Assert
import org.testng.annotations.Test

class ViewableListTest {
    @Test
    fun addRemoveAdvise() {
        val list: IMutableViewableList<Int> = ViewableList()
        val log = arrayListOf<String>()
        Lifetime.using { lifetime ->
            list.advise(lifetime) { args -> log.add("${args.kind.name} ${args.value}") }
            list.add(0)
            list.remove(0)
        }

        Assert.assertEquals(log, arrayListOf("Add 0", "Remove 0"))
    }

    @Test
    fun addRemoveView() {
        val list: IMutableViewableList<Int> = ViewableList()
        val log = arrayListOf<String>()
        Lifetime.using { lifetime ->
            list.view(lifetime) { lt, value -> log.add("View $value"); lt += { log.add("UnView $value") } }
            list.add(0)
            list.remove(0)
        }

        Assert.assertEquals(log, arrayListOf("View 0", "UnView 0"))
    }

    @Test
    fun insertInMiddle() {
        val list: IMutableViewableList<Int> = ViewableList()
        val log = arrayListOf<String>()
        Lifetime.using { lifetime ->
            list.advise(lifetime) { args -> log.add("${args.kind.name} ${args.value} at ${args.index}") }
            list.add(0)
            list.add(2)

            list.add(1, 1)
            Assert.assertEquals(log, arrayListOf("Add 0 at 0", "Add 2 at 1", "Add 1 at 1"), "add to position")
            Assert.assertEquals(list, arrayListOf(0, 1, 2), "add to position")
        }
    }

    @Test(enabled = false)//todo please rewrite this test according to change in a storage type
    fun otherReactiveApi() {
        val list: IMutableViewableList<Int> = ViewableList()
        val log = arrayListOf<String>()
        Lifetime.using { lifetime ->
            list.advise(lifetime) { args -> log.add("${args.kind.name} ${args.value}") }
            list.add(0)
            list.add(0, 1)

            Assert.assertEquals(log, arrayListOf("Add 0", "Add 1"), "add to position")
            Assert.assertEquals(list, arrayListOf(1, 0), "add to position")
            log.clear()

            list[1] = 2
            Assert.assertEquals(log, arrayListOf("Remove 0", "Add 2"), "set")
            Assert.assertEquals(list, arrayListOf(1, 2),"set")
            log.clear()

            list.clear()
            Assert.assertEquals(log, arrayListOf("Remove 2", "Remove 1"),"clear")
            Assert.assertEquals(list, arrayListOf<Int>(), "clear")
            log.clear()

            list.add(1)
            list.addAll(listOf(1, 2))
            Assert.assertEquals(log, arrayListOf("Add 1", "Add 2"), "addAll")
            Assert.assertEquals(list, arrayListOf(1, 2), "addAll")
            log.clear()

            list.addAll(1, listOf(3, 4))
            Assert.assertEquals(log, arrayListOf("Add 3", "Add 4"), "addAll at position")
            Assert.assertEquals(list, arrayListOf(1, 3, 4, 2), "addAll at position")
            log.clear()

            list.removeAll(listOf(3, 4))
            Assert.assertEquals(log, arrayListOf("Remove 3", "Remove 4"), "removeAll")
            Assert.assertEquals(list, arrayListOf(1, 2), "removeAll")
            log.clear()

            list.removeAt(0)
            Assert.assertEquals(log, arrayListOf("Remove 1"), "removeAt")
            Assert.assertEquals(list, arrayListOf(2), "removeAt")
            log.clear()

            list.retainAll(listOf(1, 2))
            Assert.assertEquals(log, arrayListOf<String>(), "retainAll1")
            Assert.assertEquals(list, arrayListOf(2), "retainAll1")
            log.clear()

            list.retainAll(listOf(1))
            Assert.assertEquals(log, arrayListOf("Remove 2"), "retainAll2")
            Assert.assertEquals(list, arrayListOf<Int>(), "retainAll2")
            log.clear()

            Assert.assertTrue(list.add(0))
            Assert.assertTrue(!list.add(0))
        }
    }
}