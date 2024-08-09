package com.jetbrains.rd.util.test.cases

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.plusAssign
import com.jetbrains.rd.util.reactive.IMutableViewableList
import com.jetbrains.rd.util.reactive.IViewableList
import com.jetbrains.rd.util.reactive.ViewableList
import com.jetbrains.rd.util.reactive.viewableTail
import com.jetbrains.rd.util.test.framework.RdTestBase
import kotlin.test.*

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
            list[0] = 1
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

    @Test
    fun testSync() {
        val items = ViewableList(mutableListOf(1, 2, 3))
        items.assertSync(listOf(1, 2, 3), emptyList(), emptyList())
        items.assertSync(listOf(3, 2, 1), listOf(2 to 1, 1 to 2), listOf(2 to 1, 1 to 0))
        items.assertSync(listOf(4, 3, 2, 1, 0), listOf(4 to 0, 0 to 4), emptyList())
        items.assertSync(listOf(3, 2, 1), emptyList(), listOf(4 to 0, 0 to 3))
        items.assertSync(listOf(4, 2, 0), listOf(4 to 0, 0 to 2), listOf(1 to 2, 3 to 0))
        items.assertSync(emptyList(), emptyList(), listOf(0 to 2, 2 to 1, 4 to 0))
        items.assertSync(listOf(1, 2, 3, 4, 5), listOf(1 to 0, 2 to 1, 3 to 2, 4 to 3, 5 to 4), emptyList())
        items.assertSync(listOf(2, 1, 3, 5, 4), listOf(1 to 1, 4 to 4), listOf(4 to 3, 1 to 0))
        items.assertSync(listOf(2, 1, 4), emptyList(), listOf(5 to 3, 3 to 2))
        items.assertSync(listOf(2, 3, 1, 5, 4), listOf(3 to 1, 5 to 3), emptyList())
        items.assertSync(listOf(2, 2, 3, 3, 1, 1, 5, 5, 4, 4), listOf(2 to 1, 3 to 3, 1 to 5, 5 to 7, 4 to 9), emptyList())
        items.assertSync(listOf(2, 2, 3, 1, 1, 5, 4, 4), emptyList(), listOf(5 to 7, 3 to 3))
        items.assertSync(listOf(2, 2, 2, 5, 5, 5), listOf(2 to 2, 5 to 4, 5 to 5), listOf(4 to 7, 4 to 6, 1 to 4, 1 to 3, 3 to 2))
        items.assertSync(listOf(2, 5), emptyList(), listOf(2 to 2, 2 to 1, 5 to 3, 5 to 2))
    }

    @Test
    fun testViewableTail() {
        val items = ViewableList(mutableListOf(1, 2, 3))
        Lifetime.using { lifetime ->
            val tail = mutableListOf<Int?>()
            items.viewableTail().advise(lifetime) { tail.add(it) }
            items.add(4)
            items.addAll(listOf(5, 6, 7))
            items.remove(6)
            items.remove(7)
            items.removeAll(listOf(2, 3, 4, 5, 6, 7))
            items.sync(listOf(2, 3))
            items.sync(listOf(1, 2))
            assertContentEquals(listOf(3, 4, 7, 5, 1, 3, 2), tail)
        }
    }

    private fun <T : Any> ViewableList<T>.assertSync(expectedItems: List<T>, expectedAdded: List<Pair<T, Int>>, expectedRemoved: List<Pair<T, Int>>) {
        assertItemsAndChanges(expectedItems, expectedAdded, expectedRemoved) {
            sync(expectedItems)
        }
    }

    private fun <T : Any> ViewableList<T>.assertItemsAndChanges(expectedItems: List<T>, expectedAdded: List<Pair<T, Int>>, expectedRemoved: List<Pair<T, Int>>, action: ViewableList<T>.() -> Unit) {
        Lifetime.using { lifetime ->
            val added = mutableListOf<Pair<T, Int>>()
            val removed = mutableListOf<Pair<T, Int>>()
            change.advise(lifetime) {
                when (it) {
                    is IViewableList.Event.Add -> added.add(it.newValue to it.index)
                    is IViewableList.Event.Remove -> removed.add(it.oldValue to it.index)
                    is IViewableList.Event.Update -> {}
                }
            }
            action()
            assertContentEquals(expectedItems, this)
            assertContentEquals(expectedAdded, added)
            assertContentEquals(expectedRemoved, removed)
        }
    }
}