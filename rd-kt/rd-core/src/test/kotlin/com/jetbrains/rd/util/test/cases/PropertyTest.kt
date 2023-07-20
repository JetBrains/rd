package com.jetbrains.rd.util.test.cases

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.plusAssign
import com.jetbrains.rd.util.reactive.*
import com.jetbrains.rd.util.test.framework.RdTestBase
import com.jetbrains.rd.util.threading.SynchronousScheduler
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull
import kotlin.test.Test

class PropertyTest  : RdTestBase() {
    @Test
    fun testAdvise() {
        var acc = 0

        val property: IProperty<Int> = Property(acc)
        property.value = ++acc

        val log = arrayListOf<Int>()
        Lifetime.using { lifetime ->
            property.advise(lifetime) { x -> log.add(-x) }
            property.view(lifetime) { inner, x -> inner.bracket({ log.add(x) }, { log.add(10 + x) }) }

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
            property.whenTrue(lifetime) { _ -> acc1++ }
            property.whenTrue(lifetime) { it.bracket({ acc2 += 2 }, { acc2 -= 1 }) }
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
        val property: IOptProperty<Boolean> = OptProperty()
        assertNull(property.valueOrNull)
        assertFails { property.valueOrThrow }

        var a = 0
        property.advise(Lifetime.Eternal, { a++ })
        assertEquals(0, a)

        property.set(false)
        assertEquals(false, property.valueOrThrow)
        assertEquals(1, a)
    }

    @Test
    fun testBoolLogic() {
        val a: IOptProperty<Boolean> = OptProperty()
        val b: IOptProperty<Boolean> = OptProperty()

        val x = a.and(b)
        assertFails { x.valueOrThrow }

        a.set(false)
        assertFails { x.valueOrThrow }

        b.set(false)
        assertEquals(false, x.valueOrThrow)

        a.set(true)
        assertEquals(false, x.valueOrThrow)

        b.set(true)
        assertEquals(true, x.valueOrThrow)

        a.set(false)
        assertEquals(false, x.valueOrThrow)
    }

    @Test
    fun testBind() {
        val p = OptProperty(0)
        val outer = OptProperty<Int>()

        Lifetime.using { lf ->
            p.bind(lf, { outer.set(it) }, { setter -> outer.advise(lf, { setter(it) }) })

            assertEquals(0, outer.valueOrThrow)

            p.set(1)
            assertEquals(1, outer.valueOrThrow)

            outer.set(2)
            assertEquals(2, p.valueOrThrow)
        }

        p.set(3)
        assertEquals(2, outer.valueOrThrow)

        outer.set(4)
        assertEquals(3, p.valueOrThrow)

    }

    @Test
    fun propertyMapDeduplicationTest() {
        Lifetime.using { lifetime ->
            val property = Property(0)
            var count  = 0
            val mappedProperty = property.map { 0 }

            mappedProperty.advise(lifetime) {
                count++
            }

            mappedProperty.adviseOn(lifetime, SynchronousScheduler) {
                count++
            }

            assertEquals(2, count)

            mappedProperty.change.advise(lifetime) {
                count++
            }

            mappedProperty.change.adviseOn(lifetime, SynchronousScheduler) {
                count++
            }

            for (i in 0 .. 10_000) {
                property.set(i)
            }

            assertEquals(2, count)
        }
    }

    @Test
    fun optPropertyMapDeduplicationTest() {
        Lifetime.using { lifetime ->
            val property = OptProperty<Int>()
            var count  = 0
            val mappedProperty = property.map { 0 }

            mappedProperty.advise(lifetime) {
                count++
            }

            mappedProperty.adviseOn(lifetime, SynchronousScheduler) {
                count++
            }

            mappedProperty.change.advise(lifetime) {
                count++
            }

            mappedProperty.change.adviseOn(lifetime, SynchronousScheduler) {
                count++
            }

            assertEquals(0, count)

            for (i in 0 .. 10_000) {
                property.set(i)
            }

            assertEquals(4, count)
        }
    }

    @Test
    fun propertySwitchMapDeduplicationTest() {
        Lifetime.using { lifetime ->
            val property = Property(Property(0))
            var count = 0
            val mappedProperty = property.switchMap { it }

            mappedProperty.advise(lifetime) {
                count++
            }

            mappedProperty.adviseOn(lifetime, SynchronousScheduler) {
                count++
            }

            assertEquals(2, count)

            mappedProperty.change.advise(lifetime) {
                count++
            }

            mappedProperty.change.adviseOn(lifetime, SynchronousScheduler) {
                count++
            }

            for (i in 0..10_000) {
                property.value = Property(0)
            }

            assertEquals(2, count)

            for (i in 0..10_000) {
                property.value = Property(1)
            }

            assertEquals(6, count)

            property.value.value = 2

            assertEquals(10, count)
        }
    }
}
