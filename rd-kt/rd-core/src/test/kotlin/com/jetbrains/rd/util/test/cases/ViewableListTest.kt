package com.jetbrains.rd.util.test.cases

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.plusAssign
import com.jetbrains.rd.util.reactive.IMutableViewableList
import com.jetbrains.rd.util.reactive.ViewableList
import com.jetbrains.rd.util.test.framework.RdTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ViewableListTest : RdTestBase()  {
    @Test
    fun addRemoveAdvise() {
        val list: IMutableViewableList<Int> = ViewableList()
        val log = arrayListOf<String>()
        Lifetime.using { lifetime ->
            list.adviseAddRemove(lifetime) { kind, idx, value -> log.add("${kind.name} $idx $value") }
            list.add(0)
            list.remove(0)
        }

        assertEquals(log, arrayListOf("Add 0 0", "Remove 0 0"))
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

        assertEquals(log, arrayListOf("View (0, 0)", "UnView (0, 0)"))
    }

    @Test
    fun addRemoveView2() {
        val list: IMutableViewableList<Int> = ViewableList()
        val log = arrayListOf<String>()
        Lifetime.using { lifetime ->
            list.view(lifetime) { lt, value -> log.add("View $value"); lt += { log.add("UnView $value") } }
            list.add(0)
            list.set(0, 1);
            list.remove(0)
        }

        assertEquals(log, arrayListOf("View (0, 0)", "UnView (0, 0)", "View (0, 1)", "UnView (0, 1)"))
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
            assertEquals(log, arrayListOf("Add 0 0", "Add 1 2", "Add 1 1"), "add to position")
            assertEquals(list.toList(), arrayListOf(0, 1, 2), "add to position")
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
            assertEquals(iterator.next(), 1)
            assertEquals(iterator.next(), 2)

            iterator.remove() // now at 1

            assertEquals(iterator.next(), 3)

            iterator.add(6)

            assertEquals(iterator.next(), 4)

            iterator.set(7)

            assertEquals(list.toList(), arrayListOf(1, 3, 6, 7, 5), "add to position")
            assertEquals(log, arrayListOf("Remove 1 2", "Add 2 6", "Remove 3 4", "Add 3 7"), "add to position")
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

            assertEquals(log, arrayListOf("Add 0 0", "Add 0 1"), "add to position")
            assertEquals(list.toList(), arrayListOf(1, 0), "add to position")
            log.clear()

            list[1] = 2
            assertEquals(log, arrayListOf("Remove 1 0", "Add 1 2"), "set")
            assertEquals(list.toList(), arrayListOf(1, 2),"set")
            log.clear()

            list.clear()
            assertEquals(log, arrayListOf("Remove 1 2", "Remove 0 1"),"clear")
            assertEquals(list.toList(), arrayListOf<Int>(), "clear")
            log.clear()

            list.add(1)
            list.addAll(listOf(1, 2))
            assertEquals(log, arrayListOf("Add 0 1", "Add 1 1", "Add 2 2"), "addAll")
            assertEquals(list.toList(), arrayListOf(1, 1, 2), "addAll")
            log.clear()

            list.addAll(1, listOf(3, 4))
            assertEquals(log, arrayListOf("Add 1 3", "Add 2 4"), "addAll at position")
            assertEquals(list.toList(), arrayListOf(1, 3, 4, 1, 2), "addAll at position")
            log.clear()

            list.removeAll(listOf(1, 3))
            assertEquals(log, arrayListOf("Remove 3 1", "Remove 1 3", "Remove 0 1"), "removeAll")
            assertEquals(list.toList(), arrayListOf(4, 2), "removeAll")
            log.clear()

            list.removeAt(0)
            assertEquals(log, arrayListOf("Remove 0 4"), "removeAt")
            assertEquals(list.toList(), arrayListOf(2), "removeAt")
            log.clear()

            list.retainAll(listOf(1, 2))
            assertEquals(log, arrayListOf<String>(), "retainAll1")
            assertEquals(list.toList(), arrayListOf(2), "retainAll1")
            log.clear()

            list.retainAll(listOf(1))
            assertEquals(log, arrayListOf("Remove 0 2"), "retainAll2")
            assertEquals(list.toList(), arrayListOf<Int>(), "retainAll2")
            log.clear()

            assertTrue(list.add(0))
            assertTrue(list.add(0))
        }
    }
}