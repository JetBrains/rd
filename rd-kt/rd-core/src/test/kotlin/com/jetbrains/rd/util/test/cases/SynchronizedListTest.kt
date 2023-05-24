package com.jetbrains.rd.util.test.cases

import com.jetbrains.rd.util.collections.SynchronizedList
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Stack
import kotlin.test.*

class SynchronizedListTest {

    @Test
    fun modificationDuringSynchronizedMapIterationTest() {
        fun doTest(action: (SynchronizedList<String>) -> Unit) {
            val strings = (0 until 10).map { it.toString() }
            val list = SynchronizedList<String>()

            fun refill() {
                list.clear()
                list.addAll(strings)
            }

            refill()
            val mapTestList = mutableListOf<String>()
            list.forEach { item ->
                if (mapTestList.isEmpty()) action(list)

                mapTestList.add(item)
                return@forEach
            }

            assertEquals(strings.size, mapTestList.size)
            for (i in strings.indices) {
                assertEquals(strings[i], mapTestList[i])
            }
        }

        doTest { map -> map.clear() }

        doTest { map ->
            map[0] = "0"
            assertEquals("0", map[0])
        }

        doTest { map ->
            map[1] = "2"
            assertEquals("2", map[1])
        }

        doTest { map ->
            map[1] = "1"
            assertEquals("1", map[1])

            for (i in 0..1) {
                assertEquals(i.toString(), map[i])
            }
        }

        doTest { map ->
            var count = 0
            map.forEachIndexed { index, item ->
                count++
                assertEquals(item, index.toString())
            }

            assertEquals(10, count)
        }

        doTest { map ->
            assertTrue(map.add("3"))
            assertEquals("3", map[map.size - 1])
        }

        doTest { map ->
            map.add(0, "3")
            assertEquals("3", map[0])
        }

        doTest { map ->
            map.clear()
            assertTrue(map.isEmpty())
        }

        val list = (100..110).map { it.toString() }
        doTest { map ->
            map.addAll(list)

            list.forEach {
                assertTrue(map.remove(it))
            }
        }

        doTest { map ->
            map.addAll(map.size - 1, list)

            list.forEach {
                assertTrue(map.remove(it))
            }
        }


        doTest { map ->
            val size = map.size
            var count = 0
            map.forEach { k ->
                count++
                map.add(map.size.toString())

                val size2 = map.size
                var count2 = 0
                map.forEachIndexed { k2, v2 ->
                    count2++
                    assertEquals(v2, k2.toString())
                }

                assertEquals(size2, count2)
            }

            assertEquals(size, count)
        }

        doTest { map ->
            val retainList = map.drop(2).take(4)
            assertTrue(map.retainAll(retainList))
            assertEquals(4, map.size)

            retainList.forEachIndexed { index, item ->
                assertEquals(item, map[index])
            }
        }

        doTest { map ->
            assertEquals("0", map.removeAt(0))
            assertEquals(9, map.size)
        }

        doTest { map ->
            val iterator = map.iterator()
            var count = 0
            while (iterator.hasNext()) {
                val entry = iterator.next()
                assertEquals(count++.toString(), entry)

                assertThrows<UnsupportedOperationException> { iterator.remove() }
                assertTrue(map.contains(entry))
            }
        }

        doTest { map ->
            for (i in 0 until map.size) {
                val iterator = map.listIterator(i)
                val stack = Stack<String>()
                var count = 0
                while (iterator.hasNext()) {
                    assertEquals(count++, iterator.nextIndex())
                    stack.push(iterator.next())

                    assertThrows<UnsupportedOperationException> { iterator.remove() }
                    assertThrows<UnsupportedOperationException> { iterator.add("123") }
                    assertThrows<UnsupportedOperationException> { iterator.set("124") }
                }

                while (iterator.hasPrevious()) {
                    assertEquals(--count, iterator.previousIndex())
                    assertEquals(stack.pop(), iterator.previous())

                    assertThrows<UnsupportedOperationException> { iterator.remove() }
                    assertThrows<UnsupportedOperationException> { iterator.add("123") }
                    assertThrows<UnsupportedOperationException> { iterator.set("124") }
                }
            }
        }

        doTest { map ->
            map.subList(2,5).forEachIndexed { index, item ->
                assertEquals(map[2 + index], item)
            }
        }

        doTest { map ->
            map.removeAll((0..5).map { it.toString() })
            assertEquals(4, map.size)
            map.forEach {
                assertEquals(it, map[it.toInt() - 6])
            }
        }

        doTest { map ->
            val size = map.size
            assertTrue(map.removeIf {
                it == "0"
            })

            assertEquals(size - 1, map.size)
            assertFalse(map.contains("0"))
        }

        doTest { map ->
            val size = map.size
            assertTrue(map.removeIf {
                if (it == "0")
                    map.remove("0")

                it == "1"
            })

            assertEquals(size - 2, map.size)
            assertFalse(map.contains("0"))
            assertFalse(map.contains("1"))
        }

        doTest { map ->
            map.replaceAll { value ->
                value + "_"
            }

            map.forEachIndexed { index, value ->
                assertEquals(index.toString() + "_", value)
            }
        }

        doTest { map ->
            map.replaceAll { value ->
                if (value == "0")
                    map.remove("0")

                value + "_"
            }

            assertFalse(map.contains("0"))
            map.forEachIndexed { index, value ->
                assertEquals((index + 1).toString() + "_", value)
            }
        }

        doTest { map ->
            map.sortWith { l,r  ->
                r.toInt().compareTo(l.toInt())
            }

            map.forEachIndexed { index, value ->
                assertEquals(map.size - index -1, value.toInt())
            }
        }
        doTest { map ->
            map.sortWith { l,r  ->
                if (l == "0" || r == "0")
                    map.remove("0")

                r.toInt().compareTo(l.toInt())
            }

            assertFalse(map.contains("0"))

            map.forEachIndexed { index, value ->
                assertEquals(map.size - index, value.toInt())
            }
        }
    }

    @Test
    fun withCustomDelegateTest() {
        val map = SynchronizedList<String>()
        assertTrue(map.isEmpty())
        doReplaceAllTest(map)
        map.clear()
        doRemoveIfAllTest(map)
    }

    private fun doRemoveIfAllTest(map: SynchronizedList<String>) {
        assertTrue(map.isEmpty())

        var count = 0
        map.removeIf { entry ->
            count++
            false
        }

        assertEquals(0, count)

        val list = (0..10).map { it.toString() }
        map.addAll(list)

        map.removeIf { entry ->
            count++
            entry.toInt() % 2 == 0
        }

        assertEquals(list.size, count)

        list.forEach { value ->
            val key = value.toInt()
            if (key % 2 == 0 )
                assertFalse(map.contains(value))
            else
                assertTrue(map.contains(value))
        }
    }

    private fun doReplaceAllTest(map: SynchronizedList<String>) {
        assertTrue(map.isEmpty())

        var count = 0
        map.replaceAll { v ->
            count++
            v
        }

        assertEquals(0, count)

        val list = (0..10).map { it.toString() }
        map.addAll(list)

        map.replaceAll { v ->
            count++
            v + v
        }

        assertEquals(list.size, count)

        list.forEach {
            assertEquals(it + it, map[it.toInt()])
        }
    }
}