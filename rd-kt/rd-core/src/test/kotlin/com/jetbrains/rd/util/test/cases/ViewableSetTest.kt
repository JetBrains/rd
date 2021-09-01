package com.jetbrains.rd.util.test.cases

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.plusAssign
import com.jetbrains.rd.util.reactive.AddRemove
import com.jetbrains.rd.util.reactive.IMutableViewableSet
import com.jetbrains.rd.util.reactive.ViewableSet
import com.jetbrains.rd.util.test.framework.RdTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ViewableSetTest : RdTestBase()  {

    @Test
    fun testAdvise() {
        val set = ViewableSet<Int>()

        val logAdvise = mutableListOf<Int>()
        val logView1 = mutableListOf<Int>()
        val logView2 = mutableListOf<Int>()
        Lifetime.using { lt ->

            set.advise(lt) {kind, v -> logAdvise.add(if (kind == AddRemove.Add) v else -v)}

            set.view(lt) { inner, v ->
                logView1.add(v)
                inner += {logView1.add(-v)}
            }

            set.view(Lifetime.Eternal) {
                inner, v ->
                logView2.add(v)
                inner += {logView2.add(-v)}
            }

            assertTrue { set.add(1) } //1
            assertTrue { set.addAll(arrayOf(1, 2)) } //1, 2
            assertFalse { set.addAll(arrayOf(1, 2)) } // 1, 2
            assertTrue { set.removeAll(arrayOf(2, 3)) } // 1

            assertTrue { set.add(2) } // 1, 2
            assertFalse { set.add(2) } // 1, 2

            assertTrue { set.retainAll(arrayOf(2, 3)) } // 2
        }

        assertTrue { set.add(1) }

        assertEquals(listOf(1, 2, -2, 2, -1), logAdvise)
        assertEquals(listOf(1, 2, -2, 2, -1, -2), logView1)
        assertEquals(listOf(1, 2, -2, 2, -1, 1), logView2)
    }

    @Test
    fun testView() {
        val elementsView = listOf(2, 0, 1, 8, 3)
        val elementsUnView = listOf(1, 3, 8, 0, 2)

        val set: IMutableViewableSet<Int> = ViewableSet()
        val log = arrayListOf<String>()
        Lifetime.using { lifetime ->
            set.view(lifetime) { lt, value -> log.add("View $value"); lt += { log.add("UnView $value") } }
            for (x in elementsView) {
                set.add(x)
            }
            set.remove(1)
        }
        val a = elementsView.map { "View $it" }
        val b = elementsUnView.map { "UnView $it" }
        assertEquals(a.union(b).toList(), log)
    }

}
