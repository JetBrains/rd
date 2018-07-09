package com.jetbrains.rider.util.test.cases

import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.reactive.Property
import kotlin.test.Test

class AdviseVsViewTest {
    @Test
    fun adviseBehavior1() {
        val lifetimeDef = Lifetime.create(Lifetime.Eternal)
        val property = Property(false)
        val lifetime = lifetimeDef.lifetime
        lifetime.add { property.set(true) }
        property.advise(lifetime) { /*lt, */value ->
            println("set to $value")
        }
        lifetimeDef.terminate()
    }

    @Test
    fun viewBehavior1() {
        val lifetimeDef = Lifetime.create(Lifetime.Eternal)
        val property = Property(false)
        val lifetime = lifetimeDef.lifetime
        lifetime.add { property.set(true) }
        property.view(lifetime) { _, value ->
            println("set to $value")
        }
        lifetimeDef.terminate()
    }

    @Test
    fun adviseBehavior2() {
        val lifetimeDef = Lifetime.create(Lifetime.Eternal)
        val property = Property(false)
        val lifetime = lifetimeDef.lifetime
        property.advise(lifetime) { /*lt, */value ->
            println("set to $value")
        }
        lifetime.add { property.set(true) }
        lifetimeDef.terminate()
    }

    @Test
    fun viewBehavior2() {
        val lifetimeDef = Lifetime.create(Lifetime.Eternal)
        val property = Property(false)
        val lifetime = lifetimeDef.lifetime
        property.view(lifetime) { _, value -> // previously would throw an exception on viewing changes from { property.set(true) }
            println("set to $value")
        }
        lifetime.add { property.set(true) }
        lifetimeDef.terminate()
    }

    @Test
    fun adviseBehavior3() {
        val lifetimeDef = Lifetime.create(Lifetime.Eternal)
        val propertyA = Property(0)
        val propertyB = Property(0)
        val lifetime = lifetimeDef.lifetime

        propertyA.advise(lifetime) { value -> println("set A to $value")}
        propertyB.advise(lifetime) { value -> println("set B to $value")}

        propertyA.set(1)
        propertyB.set(2)

        lifetime.terminate()

        propertyA.set(3)
        propertyB.set(4)
    }

    @Test
    fun viewBehavior3() {
        val lifetimeDef = Lifetime.create(Lifetime.Eternal)
        val propertyA = Property(0)
        val propertyB = Property(0)
        val lifetime = lifetimeDef.lifetime

        propertyA.view(lifetime) { _, value -> println("set A to $value")}
        propertyB.view(lifetime) { _, value -> println("set B to $value")}

        propertyA.set(1)
        propertyB.set(2)

        lifetime.terminate()

        propertyA.set(3)
        propertyB.set(4)
    }
}