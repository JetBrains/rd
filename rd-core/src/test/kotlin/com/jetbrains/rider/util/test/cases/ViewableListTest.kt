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
            list.adviseAddRemove(lifetime) { kind, idx, value -> log.add("${kind.name} $idx $value") }
            list.add(0)
            list.remove(0)
        }

        Assert.assertEquals(log, arrayListOf("Add 0 0", "Remove 0 0"))
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

        Assert.assertEquals(log, arrayListOf("View (0, 0)", "UnView (0, 0)"))
    }

    @Test
    fun insertInMiddle() {
        val list: IMutableViewableList<Int> = ViewableList()
        val log = arrayListOf<String>()
        Lifetime.using { lifetime ->
            list.adviseAddRemove(lifetime) { kind, idx, value -> log.add("${kind.name} $idx $value") }
            list.add(0)
            list.add(2)

            list.add(1, 1)
            Assert.assertEquals(log, arrayListOf("Add 0 0", "Add 1 2", "Add 1 1"), "add to position")
            Assert.assertEquals(list, arrayListOf(0, 1, 2), "add to position")
        }
    }

    @Test
    fun listIteratorTest() {
        val list: IMutableViewableList<Int> = ViewableList()
        val log = arrayListOf<String>()
        Lifetime.using { lifetime ->
            list.adviseAddRemove(lifetime) { kind, idx, value -> log.add("${kind.name} $idx $value") }
            list.addAll(listOf(1, 2, 3, 4, 5))
            log.clear()

            val iterator = list.listIterator()
            Assert.assertEquals(iterator.next(), 1)
            Assert.assertEquals(iterator.next(), 2)

            iterator.remove() // now at 1

            Assert.assertEquals(iterator.next(), 3)

            iterator.add(6)

            Assert.assertEquals(iterator.next(), 4)

            iterator.set(7)

            Assert.assertEquals(list, arrayListOf(1, 3, 6, 7, 5), "add to position")
            Assert.assertEquals(log, arrayListOf("Remove 1 2", "Add 2 6", "Remove 3 4", "Add 3 7"), "add to position")
        }
    }

    @Test()
    fun otherReactiveApi() {
        val list: IMutableViewableList<Int> = ViewableList()
        val log = arrayListOf<String>()
        Lifetime.using { lifetime ->
            list.adviseAddRemove(lifetime) { kind, idx, value -> log.add("${kind.name} $idx $value") }
            list.add(0)
            list.add(0, 1)

            Assert.assertEquals(log, arrayListOf("Add 0 0", "Add 0 1"), "add to position")
            Assert.assertEquals(list, arrayListOf(1, 0), "add to position")
            log.clear()

            list[1] = 2
            Assert.assertEquals(log, arrayListOf("Remove 1 0", "Add 1 2"), "set")
            Assert.assertEquals(list, arrayListOf(1, 2),"set")
            log.clear()

            list.clear()
            Assert.assertEquals(log, arrayListOf("Remove 1 2", "Remove 0 1"),"clear")
            Assert.assertEquals(list, arrayListOf<Int>(), "clear")
            log.clear()

            list.add(1)
            list.addAll(listOf(1, 2))
            Assert.assertEquals(log, arrayListOf("Add 0 1", "Add 1 1", "Add 2 2"), "addAll")
            Assert.assertEquals(list, arrayListOf(1, 1, 2), "addAll")
            log.clear()

            list.addAll(1, listOf(3, 4))
            Assert.assertEquals(log, arrayListOf("Add 1 3", "Add 2 4"), "addAll at position")
            Assert.assertEquals(list, arrayListOf(1, 3, 4, 1, 2), "addAll at position")
            log.clear()

            list.removeAll(listOf(1, 3))
            Assert.assertEquals(log, arrayListOf("Remove 3 1", "Remove 1 3", "Remove 0 1"), "removeAll")
            Assert.assertEquals(list, arrayListOf(4, 2), "removeAll")
            log.clear()

            list.removeAt(0)
            Assert.assertEquals(log, arrayListOf("Remove 0 4"), "removeAt")
            Assert.assertEquals(list, arrayListOf(2), "removeAt")
            log.clear()

            list.retainAll(listOf(1, 2))
            Assert.assertEquals(log, arrayListOf<String>(), "retainAll1")
            Assert.assertEquals(list, arrayListOf(2), "retainAll1")
            log.clear()

            list.retainAll(listOf(1))
            Assert.assertEquals(log, arrayListOf("Remove 0 2"), "retainAll2")
            Assert.assertEquals(list, arrayListOf<Int>(), "retainAll2")
            log.clear()

            Assert.assertTrue(list.add(0))
            Assert.assertTrue(list.add(0))
        }
    }
}