package com.jetbrains.rd.util.test.cases

import com.jetbrains.rd.util.collections.SynchronizedMap
import com.jetbrains.rd.util.test.framework.RdTestBase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.*

class SynchronizedMapTest : RdTestBase() {

    @Test
    fun synchronizedMapSimpleTest() {
        val map = SynchronizedMap<Int, String>()
        doSimpleMapModifications(map)
        map.clear()
        doSimpleMapEntriesModifications(map)

        map.clear()
        doSimpleKeyValueEntriesModifications(map)
    }

    @Test
    fun modificationDuringSynchronizedMapIterationTest() {
        fun doTest(action: (SynchronizedMap<Int, String>) -> Unit) {
            val list = (0 until 10).map { it to it.toString() }
            val map = SynchronizedMap<Int, String>()

            fun refill() {
                map.clear()
                map.addAll(list.map { it.toMutableEntry() })
            }

            refill()
            val mapTestList = mutableListOf<Pair<Int, String>>()
            map.forEach { key, value ->
                if (mapTestList.isEmpty()) action(map)

                mapTestList.add(Pair(key, value))
                return@forEach
            }

            assertEquals(list.size, mapTestList.size)
            for (i in list.indices) {
                assertEquals(list[i].first, mapTestList[i].first)
                assertEquals(list[i].second, mapTestList[i].second)
            }

            refill()
            val entriesTestList = mutableListOf<MutableMap.MutableEntry<Int, String>>()
            map.entries.forEach { entry ->
                if (entriesTestList.isEmpty()) action(map)

                entriesTestList.add(entry)
                return@forEach
            }

            assertEquals(list.size, entriesTestList.size)
            for (i in list.indices) {
                assertEquals(list[i].first, entriesTestList[i].key)
                assertEquals(list[i].second, entriesTestList[i].value)
            }

            refill()
            val keysTestList = mutableListOf<Int>()
            map.keys.forEach { key ->
                if (keysTestList.isEmpty()) action(map)

                keysTestList.add(key)
                return@forEach
            }

            assertEquals(list.size, keysTestList.size)
            for (i in list.indices) {
                assertEquals(list[i].first, keysTestList[i])
            }

            refill()
            val valuesTestList = mutableListOf<String>()
            map.values.forEach { value ->
                if (valuesTestList.isEmpty()) action(map)

                valuesTestList.add(value)
                return@forEach
            }

            assertEquals(list.size, valuesTestList.size)
            for (i in list.indices) {
                assertEquals(list[i].second, valuesTestList[i])
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
            map.forEach { key, value ->
                count++
                assertEquals(value, key.toString())
            }

            assertEquals(10, count)
        }

        doTest { map ->
            assertEquals("2", map.put(2, "3"))
        }

        doTest { map ->
            map.clear()
            assertTrue(map.isEmpty())
        }

        val list = (100..110).map { it to it.toString() }
        doTest { map ->
            map.putAll(list)

            list.forEach {
                assertEquals(it.second, map.remove(it.first))
            }
        }

        doTest { map ->
            assertTrue(map.addAll(list.map { it.toMutableEntry() }))
        }

        doTest { map ->
            map.forEach { k, v ->
                assertFalse(map.remove(k, v + "_"))
            }
        }

        doTest { map ->
            map.forEach { k, v ->
                assertTrue(map.remove(k, v))
            }
        }

        doTest { map ->
            assertNull(map.putIfAbsent(list.first().first, list.first().second))
        }

        doTest { map ->
            assertTrue(map.addAll(list.map { it.toMutableEntry() }))
        }

        doTest { map ->
            assertEquals("0", map.replace(0, "123"))
        }

        doTest { map ->
            assertTrue(map.replace(0, "0", "123"))
        }

        doTest { map ->
            val size = map.size
            var count = 0
            map.forEach { k, v ->
                count++
                map[map.size] = map.size.toString()

                val size2 = map.size
                var count2 = 0
                map.forEach { k2, v2 ->
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

            retainList.forEach {
                assertEquals(it.value, map[it.key])
            }
        }

        doTest { map ->
            assertThrows<UnsupportedOperationException> { map.keys.add(0) }
            assertThrows<UnsupportedOperationException> { map.values.add("0") }

            assertThrows<UnsupportedOperationException> { map.keys.addAll(0..10) }
            assertThrows<UnsupportedOperationException> { map.values.addAll((0..10).map { it.toString() }) }
        }

        doTest { map ->
            map.keys.clear()
            assertTrue(map.isEmpty())
        }

        doTest { map ->
            map.values.clear()
            assertTrue(map.isEmpty())
        }

        doTest { map ->
            map.keys.removeAll(0..5)
            assertEquals(4, map.size)
            map.keys.forEach {
                assertEquals(it.toString(), map[it])
            }
        }

        doTest { map ->
            map.values.removeAll((0..5).map { it.toString() })
            assertEquals(4, map.size)
            map.values.forEach {
                assertEquals(it, map[it.toInt()])
            }
        }

        doTest { map ->
            map.remove(0)
            assertEquals(9, map.size)
        }

        doTest { map ->
            map.values.remove("0")
            assertEquals(9, map.size)
        }

        doTest { map ->
            val iterator = map.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                assertEquals(entry.value, entry.key.toString())
                assertEquals(entry.value, map[entry.key])

                iterator.remove()
                assertNull(map[entry.key])
            }
        }

        doTest { map ->
            val iterator = map.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                assertEquals(entry.value, entry.key.toString())
                assertEquals(entry.value, map[entry.key])

                map.remove(entry.key)
                assertNull(map[entry.key])

                val snapshot = map.toMap()
                iterator.remove()

                snapshot.forEach {
                    assertTrue(map.containsKey(it.key))
                    assertTrue(map.containsValue(it.value))
                    assertEquals(it.value, map[it.key])
                }
            }
        }

        doTest { map ->
            val iterator = map.keys.iterator()
            while (iterator.hasNext()) {
                val key = iterator.next()
                assertEquals(key.toString(), map[key])

                iterator.remove()
                assertNull(map[key])
            }
        }

        doTest { map ->
            val iterator = map.values.iterator()
            while (iterator.hasNext()) {
                val value = iterator.next()
                assertEquals(value, map[value.toInt()])

                iterator.remove()
                assertNull(map[value.toInt()])
            }
        }

        doTest { map ->
            val size = map.size
            assertTrue(map.removeIf {
                it.key == 0
            })

            assertEquals(size - 1, map.size)
            assertFalse(map.containsKey(0))
        }

        doTest { map ->
            val size = map.size
            assertTrue(map.removeIf {
                if (it.key == 0)
                    map.remove(0)
                it.key == 1
            })

            assertEquals(size - 2, map.size)
            assertFalse(map.containsKey(0))
            assertFalse(map.containsKey(1))
        }

        doTest { map ->
            map.replaceAll { key, value ->
                value + "_"
            }

            map.forEach {key, value ->
                assertEquals(key.toString() + "_", value)
            }
        }

        doTest { map ->
            map.replaceAll { key, value ->
                if (key == 0)
                    map.remove(0)

                value + "_"
            }

            assertFalse(map.containsKey(0))
            map.forEach {key, value ->
                assertEquals(key.toString() + "_", value)
            }
        }
    }

    @Test
    fun withCustomDelegateTest() {
        val map = SynchronizedMap<Int, String>()
        assertTrue(map.isEmpty())
        doComputeTest(map)
        map.clear()
        doComputeIfAbsentTest(map)
        map.clear()
        doComputeIfPresentTest(map)
        map.clear()
        doMergeTest(map)
        map.clear()
        doReplaceAllTest(map)
        map.clear()
        doRemoveIfAllTest(map)
    }

    private fun doRemoveIfAllTest(map: SynchronizedMap<Int, String>) {
        assertTrue(map.isEmpty())

        var count = 0
        map.removeIf { entry ->
            count++
            false
        }

        assertEquals(0, count)

        val list = (0..10).map { it.toPair().toMutableEntry() }
        map.addAll(list)

        map.removeIf { entry ->
            count++
            entry.key % 2 == 0
        }

        assertEquals(list.size, count)

        list.forEach {
            if (it.key % 2 == 0 )
                assertFalse(map.containsKey(it.key))
            else
                assertEquals(it.value, map[it.key])
        }

        count = 0
        map.clear()
        map.addAll(list)

        map.removeIf { entry ->
            count++

            if (count < list.size)
                map[list.size + count] = "_"

            var count2 = 0
            var count3 = 0
            map.forEach { k, v ->
                count2++
                if (k > list.size) {
                    count3++
                    assertEquals("_", v)
                }
            }
            assertTrue(count2 > 0)
            assertTrue(count3 > 0)
            assertTrue(count2 > count3)

            entry.value != "_"
        }

        assertTrue(count > 0)

        list.forEach {
            assertFalse(map.containsKey(it.key))
        }

        count = 0
        map.forEach { k, v ->
            count++
            assertEquals("_", v)
        }

        assertTrue(count > 0)
    }

    private fun doReplaceAllTest(map: SynchronizedMap<Int, String>) {
        assertTrue(map.isEmpty())

        var count = 0
        map.replaceAll { k, v ->
            count++
            v
        }

        assertEquals(0, count)

        val list = (0..10).map { it.toPair().toMutableEntry() }
        map.addAll(list)

        map.replaceAll { k, v ->
            count++
            v + k
        }

        assertEquals(list.size, count)

        list.forEach {
            assertEquals(it.value + it.key, map[it.key])
        }

        count = 0
        map.clear()
        map.addAll(list)

        map.replaceAll { k, v ->
            count++

            if (count < list.size)
                map[list.size + count] = "_"

            v + k
        }

        list.forEach {
            assertEquals(it.value + it.key, map[it.key])
        }
    }


    private fun doMergeTest(map: SynchronizedMap<Int, String>) {
        assertTrue(map.isEmpty())

        var count = 0
        assertEquals("0", map.merge(0, "0") { oldValue, value ->
            count++
            fail("Must not be reached")
        })
        assertEquals(0, count)

        map[0] = "1"

        assertEquals("3", map.merge(0, "2") { oldValue, value ->
            count++
            assertEquals("1", oldValue)
            assertEquals("2", value)
            "3"
        })
        assertEquals(1, count)

        count = 0
        assertNull(map.merge(0, "4") { oldValue, value ->
            count++
            assertEquals("3", oldValue)
            assertEquals("4", value)
            null
        })
        assertEquals(1, count)

        assertFalse(map.containsKey(0))
    }


    private fun doComputeIfPresentTest(map: SynchronizedMap<Int, String>) {
        assertTrue(map.isEmpty())

        var count = 0
        assertNull(map.computeIfPresent(0) { key, value ->
            count++
            key.toString()
        })
        assertEquals(0, count)

        map[0] = "1"

        assertEquals("123", map.computeIfPresent(0) { key, value ->
            count++
            assertEquals("1", value)
            "123"
        })
        assertEquals(1, count)

        count = 0
        assertNull(map.computeIfPresent(0) { key, value ->
            count++
            assertEquals("123", value)

            map.remove(0)
            "123"
        })
        assertEquals(1, count)
        assertFalse(map.containsKey(0))

        count = 0
        map[0] = "1"
        assertNull(map.computeIfPresent(0) { key, value ->
            count++
            assertEquals("1", value)
            null
        })
        assertEquals(1, count)
        assertFalse(map.containsKey(0))
    }

    private fun doComputeIfAbsentTest(map: SynchronizedMap<Int, String>) {
        assertTrue(map.isEmpty())

        var count = 0
        for (i in 0 until 2) {
            count = 0
            assertEquals("0", map.computeIfAbsent(0) { key ->
                count++

                if (i == 1)
                    map[123] = "123"

                key.toString()
            })
            assertEquals(1, count)
            count = 0

            map.clear()

            assertEquals("1", map.computeIfAbsent(0) { key ->
                count++

                if (i == 1)
                    map[124] = "124"

                "1"
            })
            assertEquals(1, count)
            count = 0

            map.clear()
        }

        map.clear()
        count = 0
        assertEquals("123", map.computeIfAbsent(0) { key ->
            count++

            map[0] = "123"

            key.toString()
        })

        assertEquals(1, count)
        assertEquals("123", map[0])
    }


    private fun doComputeTest(map: SynchronizedMap<Int, String>) {
        assertTrue(map.isEmpty())

        var count = 0
        for (i in 0 until 2) {
            count = 0
            assertEquals("0", map.compute(0) { key, value ->
                count++
                assertNull(value)

                if (i == 1)
                    map[123] = "123"

                key.toString()
            })
            assertEquals(1, count)
            count = 0

            assertEquals("1", map.compute(0) { key, value ->
                count++
                assertEquals("0", value)

                if (i == 1)
                    map[124] = "124"

                "1"
            })
            assertEquals(1, count)
            count = 0

            assertNull(map.compute(0) { key, value ->
                count++
                assertEquals("1", value)

                if (i == 1)
                    map[125] = "125"

                null
            })
            assertEquals(1, count)
            assertFalse(map.containsKey(0))

            map.clear()
        }

        count = 0
        assertEquals("0", map.compute(0) { key, value ->
            count++
            if (count == 1)
                assertNull(value)
            else
                assertEquals("123", value)

            map[0] = "123"

            key.toString()
        })

        assertEquals(2, count)
        assertEquals("0", map[0])
    }

    private fun doSimpleKeyValueEntriesModifications(map: SynchronizedMap<Int, String>) {
        assertTrue(map.isEmpty())

        val list = (0..10).map { it.toPair().toMutableEntry() }
        map.addAll(list)
        val keys = map.keys
        val values = map.values

        assertThrows<UnsupportedOperationException> { keys.add(0) }
        assertThrows<UnsupportedOperationException> { values.add("0") }

        assertThrows<UnsupportedOperationException> { keys.addAll(0..10) }
        assertThrows<UnsupportedOperationException> { values.addAll((0..10).map { it.toString() }) }

        keys.clear()
        assertTrue(map.isEmpty())
        map.addAll(list)

        values.clear()
        assertTrue(map.isEmpty())
        map.addAll(list)

        keys.removeAll(0..5)
        assertEquals(5, map.size)
        keys.forEach {
            assertEquals(it.toString(), map[it])
        }

        values.removeAll((7..10).map { it.toString() })
        assertEquals(1, map.size)
        assertEquals(values.single(), map.single().value)


        map.addAll(list)
        keys.remove(0)
        assertEquals(10, map.size)

        map.addAll(list)
        values.remove("0")
        assertEquals(10, map.size)
    }


    private fun doSimpleMapEntriesModifications(map: SynchronizedMap<Int, String>) {
        val entries = map.entries
        assertEquals(0, entries.size)
        assertEquals(0, map.size)
        assertTrue(entries.isEmpty())
        entries.add(0.toPair().toMutableEntry())

        assertEquals(1, entries.size)
        assertEquals(1, map.size)
        assertFalse(entries.isEmpty())
        assertEquals("0", map[0])

        entries.add(Pair(1, "2").toMutableEntry())
        assertEquals(2, entries.size)
        assertEquals(2, map.size)
        assertEquals("2", map[1])


        entries.add(Pair(1, "1").toMutableEntry())
        assertEquals(2, entries.size)
        assertEquals(2, map.size)
        assertEquals("1", map[1])

        for (i in 0..1) {
            assertEquals(i.toString(), map[i])
        }

        var count = 0
        entries.forEach { entry ->
            count++
            assertEquals(entry.value, entry.key.toString())
        }

        assertEquals(2, count)

        assertTrue(entries.add(Pair(2, "3").toMutableEntry()))
        assertEquals("3", map.put(2, "2"))

        assertNull(map[3])
        assertTrue(map.containsKey(2))
        assertTrue(map.containsValue("2"))
        assertFalse(map.containsKey(3))
        assertFalse(map.containsValue("3"))

        entries.clear()
        assertTrue(entries.isEmpty())
        assertTrue(map.isEmpty())

        val list = (0..10).map { it to it.toString() }
        entries.addAll(list.map { it.toMutableEntry() })
        assertEquals(list.size, entries.size)

        list.forEach {
            assertEquals(it.second, map.remove(it.first))
        }
        assertTrue(entries.isEmpty())

        assertTrue(entries.addAll(list.map { it.toMutableEntry() }))
        assertFalse(entries.addAll(list.map { it.toMutableEntry() }))

        list.forEach {
            assertFalse(entries.remove(Pair(it.first, it.second + "_").toMutableEntry()))
            assertTrue(entries.remove(Pair(it.first, it.second).toMutableEntry()))
        }

        assertTrue(entries.isEmpty())

        assertNull(map.putIfAbsent(list.first().first, list.first().second))
        assertTrue(entries.addAll(list.map { it.toMutableEntry() }))
        assertEquals(list.first().second, map.putIfAbsent(list.first().first, list.first().second + "_"))
        assertEquals(list.first().second, map[list.first().first])
    }


    private fun doSimpleMapModifications(map: SynchronizedMap<Int, String>) {
        assertEquals(0, map.size)
        assertTrue(map.isEmpty())
        map[0] = "0"
        assertEquals(1, map.size)
        assertFalse(map.isEmpty())
        assertEquals(1, map.keys.size)
        assertEquals(1, map.values.size)
        assertEquals(1, map.entries.size)
        assertEquals("0", map[0])
        assertEquals("0", map[0])

        map[1] = "2"
        assertEquals(2, map.size)
        assertEquals(2, map.keys.size)
        assertEquals(2, map.values.size)
        assertEquals(2, map.entries.size)
        assertEquals("2", map[1])

        map[1] = "1"
        assertEquals(2, map.size)
        assertEquals(2, map.keys.size)
        assertEquals(2, map.values.size)
        assertEquals(2, map.entries.size)
        assertEquals("1", map[1])

        for (i in 0..1) {
            assertEquals(i.toString(), map[i])
        }

        var count = 0
        map.forEach { key, value ->
            count++
            assertEquals(value, key.toString())
        }

        assertEquals(2, count)

        assertNull(map.put(2, "3"))
        assertEquals("3", map.put(2, "2"))

        assertNull(map[3])
        assertTrue(map.containsKey(2))
        assertTrue(map.containsValue("2"))
        assertFalse(map.containsKey(3))
        assertFalse(map.containsValue("3"))

        map.clear()
        assertTrue(map.isEmpty())

        val list = (0..10).map { it to it.toString() }
        map.putAll(list)
        assertEquals(list.size, map.size)

        list.forEach {
            assertEquals(it.second, map.remove(it.first))
        }
        assertTrue(map.isEmpty())

        assertTrue(map.addAll(list.map { it.toMutableEntry() }))
        assertFalse(map.addAll(list.map { it.toMutableEntry() }))

        list.forEach {
            assertFalse(map.remove(it.first, it.second + "_"))
            assertTrue(map.remove(it.first, it.second))
        }

        assertTrue(map.isEmpty())

        assertNull(map.putIfAbsent(list.first().first, list.first().second))
        assertTrue(map.addAll(list.map { it.toMutableEntry() }))
        assertEquals(list.first().second, map.putIfAbsent(list.first().first, list.first().second + "_"))
        assertEquals(list.first().second, map[list.first().first])

        assertEquals("0", map.replace(0, "123"))
        assertFalse(map.replace(0, "0", "123"))
        assertTrue(map.replace(0, "123", "1234"))

        assertEquals("1234", map.getOrDefault(0, "1"))
        assertEquals("_", map.getOrDefault(1234, "_"))
        assertFalse(map.containsKey(1234))

        map.clear()
        map.addAll(list.map { it.toMutableEntry() })

        val retainList = list.drop(2).take(4)
        assertTrue(map.retainAll(retainList.map { it.toMutableEntry() }))
        assertEquals(4, map.size)

        retainList.forEach {
            assertEquals(it.second, map[it.first])
            assertFalse(map.remove(it.first, it.second + "_"))
            assertTrue(map.remove(it.first, it.second))
        }
    }

    private fun Int.toPair() = this to this.toString()

    private fun <TK, TV> Pair<TK, TV>.toMutableEntry() = object : MutableMap.MutableEntry<TK, TV> {
        override val key: TK = first

        override var value: TV = second

        override fun setValue(newValue: TV): TV {
            val old = value
            value = newValue
            return old
        }
    }
}