package com.jetbrains.rd.util.test.cases

import com.jetbrains.rd.util.collections.SynchronizedSet
import org.junit.jupiter.api.Test
import kotlin.test.*

class SynchronizedSetTest {

    @Test
    fun modificationDuringSynchronizedSetIterationTest() {
        fun doTest(action: (SynchronizedSet<Int>) -> Unit) {
            val list = (0 until 10).toList()
            val set = SynchronizedSet<Int>()
            set.addAll(list)

            val setTestList = mutableListOf<Int>()
            set.forEach { key ->
                if (setTestList.isEmpty()) action(set)

                setTestList.add(key)
                return@forEach
            }

            assertEquals(list.size, setTestList.size)
            for (i in list.indices) {
                assertEquals(list[i], setTestList[i])
            }
        }

        doTest { set -> set.clear() }

        doTest { set ->
            var count = 0
            set.forEach { key ->
                count++
            }

            assertEquals(10, count)
        }

        doTest { set ->
            assertFalse(set.add(2))
        }

        doTest { set ->
            assertTrue(set.add(222))
        }

        doTest { set ->
            set.clear()
            assertTrue(set.isEmpty())
        }

        val list = (100..110).toList()
        doTest { set ->
            val size = set.size
            assertTrue(set.addAll(list))

            assertEquals(size + list.size, set.size)
            list.forEach {
                assertTrue(set.contains(it))
            }
        }

        doTest { set ->
            set.forEach { k ->
                assertTrue(set.remove(k))
            }
        }

                doTest { set ->
            val size = set.size
            var count = 0
            set.forEach { k ->
                count++
                assertTrue(set.add(set.size))

                val size2 = set.size
                var count2 = 0
                set.forEach { k2 ->
                    count2++
                }

                assertEquals(size2, count2)
            }

            assertEquals(size, count)
        }

        doTest { set ->
            val retainList = set.drop(2).take(4)
            assertTrue(set.retainAll(retainList))
            assertEquals(4, set.size)

            retainList.forEach {
                assertTrue(set.contains(it))
            }
        }

        doTest { set ->
            set.remove(0)
            assertEquals(9, set.size)
        }

        doTest { set ->
            val iterator = set.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                assertTrue(set.contains(entry))

                iterator.remove()
                assertFalse(set.contains(entry))
            }
        }
    }

    private fun doRemoveIfAllTest(set: SynchronizedSet<Int>) {
        assertTrue(set.isEmpty())

        var count = 0
        set.removeIf { entry ->
            count++
            false
        }

        assertEquals(0, count)

        val list = (0..10).toList()
        set.addAll(list)

        set.removeIf { entry ->
            count++
            entry % 2 == 0
        }

        assertEquals(list.size, count)

        list.forEach {
            if (it % 2 == 0 )
                assertFalse(set.contains(it))
            else
                assertTrue(set.contains(it))
        }

        count = 0
        set.clear()
        set.addAll(list)

        set.removeIf { entry ->
            count++

            set.add(123)

            var count2 = 0
            var count3 = 0
            set.forEach { k ->
                count2++
                if (k > list.size) {
                    count3++
                    assertEquals(123, k)
                }
            }
            assertTrue(count2 > 0)
            assertEquals(1, count3)
            assertTrue(count2 > count3)

            entry != 123
        }

        assertTrue(count > 0)

        list.forEach {
            assertFalse(set.contains(it))
        }

        count = 0
        set.forEach { k ->
            count++
            assertEquals(213, k)
        }

        assertTrue(count > 0)
    }
}