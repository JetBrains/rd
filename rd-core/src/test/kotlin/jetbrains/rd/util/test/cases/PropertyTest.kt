package com.jetbrains.rider.util.test.cases

import com.jetbrains.rider.util.Maybe
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.lifetime.plusAssign
import com.jetbrains.rider.util.reactive.*
import org.testng.annotations.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotEquals

class PropertyTest {
    @Test
    fun testAdvise() {
        var acc = 0

        val property: IProperty<Int> = Property(acc)
        property.value = ++acc

        val log = arrayListOf<Int>()
        Lifetime.using { lifetime ->
            property.advise(lifetime, { x -> log.add(-x) })
            property.view(lifetime, { inner, x -> inner.bracket({ log.add(x) }, { log.add(10 + x) }) })

            lifetime += { log.add(0) }

            property.value = property.value
            property.value = ++acc
            property.value = ++acc
        }
        property.value = ++acc

        assertEquals(listOf(-1, 1, -2, 11, 2, -3, 12, 3, 0, 13), log)
    }

    @Test
    fun testWhenTrue() {
        var acc1 = 0
        var acc2 = 0

        val property: IProperty<Boolean> = Property(false)
        property.value = true
        Lifetime.using { lifetime ->
            property.whenTrue(lifetime, { lf -> acc1++ })
            property.whenTrue(lifetime, { it.bracket({ acc2 += 2 }, { acc2 -= 1 }) })
            assertEquals(1, acc1);assertEquals(2, acc2);

            property.value = true
            assertEquals(1, acc1);assertEquals(2, acc2);

            property.value = false
            assertEquals(1, acc1);assertEquals(1, acc2);

            property.value = true
            assertEquals(2, acc1);assertEquals(3, acc2);
        }
        assertEquals(2, acc1);assertEquals(2, acc2);
    }

    @Test
    fun testMaybe() {
        val property: IProperty<Boolean> = Property()
        assertEquals(Maybe.None, property.maybe)
        assertFails { property.value }

        var a = 0
        property.advise(Lifetime.Eternal, { a++ })
        assertEquals(0, a)

        property.set(false)
        assertNotEquals(Maybe.None, property.maybe)
        assertEquals(false, property.value)
        assertEquals(1, a)
    }

    @Test
    fun testBoolLogic() {
        val a: IProperty<Boolean> = Property()
        val b: IProperty<Boolean> = Property()

        val x = a.and(b)
        assertFails { x.value }

        a.set(false)
        assertFails { x.value }

        b.set(false)
        assertEquals(false, x.value)

        a.set(true)
        assertEquals(false, x.value)

        b.set(true)
        assertEquals(true, x.value)

        a.set(false)
        assertEquals(false, x.value)
    }

    @Test
    fun testBind() {
        val p = Property(0)
        val outer = Property<Int>()

        Lifetime.using { lf ->
            p.bind(lf, { outer.set(it) }, { setter -> outer.advise(lf, { setter(it) }) })

            assertEquals(0, outer.value)

            p.value = 1
            assertEquals(1, outer.value)

            outer.value = 2
            assertEquals(2, p.value)
        }

        p.value = 3
        assertEquals(2, outer.value)

        outer.value = 4
        assertEquals(3, p.value)

    }
}
