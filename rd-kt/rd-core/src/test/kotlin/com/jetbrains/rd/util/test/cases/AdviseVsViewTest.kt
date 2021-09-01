package com.jetbrains.rd.util.test.cases

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.lifetime.onTermination
import com.jetbrains.rd.util.reactive.Property
import com.jetbrains.rd.util.test.framework.RdTestBase
import kotlin.test.Test
import kotlin.test.assertEquals

class AdviseVsViewTest : RdTestBase() {
    @Test
    fun adviseBehavior1() {
        val lifetimeDef = Lifetime.Eternal.createNested()
        val property = Property(false)
        val lifetime = lifetimeDef.lifetime

        val log = arrayListOf<Boolean>()

        lifetime.onTermination { property.set(true) }
        property.advise(lifetime) { /*lt, */value ->
            log.add(value)
        }
        lifetimeDef.terminate()

        assertEquals(listOf(false), log)
    }

    @Test
    fun viewBehavior1() {
        val lifetimeDef = Lifetime.Eternal.createNested()
        val property = Property(false)
        val lifetime = lifetimeDef.lifetime

        val log = arrayListOf<Boolean>()

        lifetime.onTermination { property.set(true) }
        property.view(lifetime) { _, value ->
            log.add(value)
        }
        lifetimeDef.terminate()

        assertEquals(listOf(false), log)
    }

    @Test
    fun adviseBehavior2() {
        val lifetimeDef = Lifetime.Eternal.createNested()
        val property = Property(false)
        val lifetime = lifetimeDef.lifetime

        val log = arrayListOf<Boolean>()

        property.advise(lifetime) { /*lt, */value ->
            log.add(value)
        }
        lifetime.onTermination { property.set(true) }
        lifetimeDef.terminate()

        assertEquals(listOf(false, true), log)
    }

    @Test
    fun viewBehavior2() {
        val lf = LifetimeDefinition()
        val property = Property(false)

        val log = arrayListOf<Boolean>()

        property.view(lf) { _, value ->
            // previously would throw an exception on viewing changes from { property.set(true) }
            log.add(value)
        }
        lf.onTermination { property.set(true) }
        lf.terminate()

        assertEquals(listOf(false), log)
    }

    @Test
    fun adviseBehavior3() {
        val lifetimeDef = Lifetime.Eternal.createNested()
        val propertyA = Property(0)
        val propertyB = Property(0)
        val lifetime = lifetimeDef.lifetime

        val logA = arrayListOf<Int>()
        val logB = arrayListOf<Int>()

        propertyA.advise(lifetime) { value -> logA.add(value) }
        propertyB.advise(lifetime) { value -> logB.add(value) }

        propertyA.set(1)
        propertyB.set(2)

        lifetimeDef.terminate()

        propertyA.set(3)
        propertyB.set(4)

        assertEquals(listOf(0, 1), logA)
        assertEquals(listOf(0, 2), logB)
    }

    @Test
    fun viewBehavior3() {
        val lifetimeDef = Lifetime.Eternal.createNested()
        val propertyA = Property(0)
        val propertyB = Property(0)
        val lifetime = lifetimeDef.lifetime

        val logA = arrayListOf<Int>()
        val logB = arrayListOf<Int>()

        propertyA.view(lifetime) { _, value -> logA.add(value) }
        propertyB.view(lifetime) { _, value -> logB.add(value) }

        propertyA.set(1)
        propertyB.set(2)

        lifetimeDef.terminate()

        propertyA.set(3)
        propertyB.set(4)

        assertEquals(listOf(0, 1), logA)
        assertEquals(listOf(0, 2), logB)
    }
}