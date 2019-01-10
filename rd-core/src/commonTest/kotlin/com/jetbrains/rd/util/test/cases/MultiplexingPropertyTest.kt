package com.jetbrains.rd.util.test.cases

import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.reactive.Property
import com.jetbrains.rd.util.wrappers.MultiplexingProperty
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MultiplexingPropertyTest {
    private lateinit var lifetimeDefinition: LifetimeDefinition
    private lateinit var prop1: Property<Int?>
    private lateinit var prop2: Property<Int?>
    private lateinit var multProp: MultiplexingProperty<Int, String>

    @BeforeTest
    fun setup() {
        lifetimeDefinition = LifetimeDefinition()
        prop1 = Property(1)
        prop2 = Property(20)
        multProp = MultiplexingProperty(lifetimeDefinition.lifetime) { it.toString().length.toString() }
    }

    @Test
    fun testChange() {
        val changeLog = mutableListOf<Int?>()
        multProp.addComponent(prop1, "1")
        multProp.addComponent(prop2, "2")
        assertEquals(1, multProp.value)

        multProp.change.advise(lifetimeDefinition.lifetime) {
            changeLog += it
        }
        prop1.value = 2
        assertEquals(listOf<Int?>(2), changeLog)
        assertEquals(2, multProp.value)
        prop2.value = 21
        assertEquals(listOf<Int?>(2, 21), changeLog)
        assertEquals(21, multProp.value)
    }

    @Test
    fun testSet() {
        multProp.addComponent(prop1, "1")
        multProp.addComponent(prop2, "2")

        multProp.set(5)
        assertEquals(5, prop1.value)
        assertEquals(null, prop2.value)

        multProp.set(25)
        assertEquals(null, prop1.value)
        assertEquals(25, prop2.value)
    }

    @Test
    fun testUnsubscribe() {
        val lt1 = LifetimeDefinition()
        val lt2 = LifetimeDefinition()
        multProp.addComponent(prop1, "1", lt1.lifetime)
        multProp.addComponent(prop2, "2", lt2.lifetime)

        val changeLog = mutableListOf<Int?>()
        multProp.change.advise(lifetimeDefinition.lifetime) {
            changeLog += it
        }

        prop2.value = 21
        assertEquals(listOf<Int?>(21), changeLog)
        lt2.terminate()
        assertEquals(listOf<Int?>(21, 1), changeLog)
        assertEquals(1, multProp.value)
        prop2.value = 22
        assertEquals(listOf<Int?>(21, 1), changeLog)
    }
}
