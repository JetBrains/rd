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
            print("set to $value")
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
            print("set to $value")
        }
        lifetimeDef.terminate()
    }

    @Test
    fun adviseBehavior2() {
        val lifetimeDef = Lifetime.create(Lifetime.Eternal)
        val property = Property(false)
        val lifetime = lifetimeDef.lifetime
        property.advise(lifetime) { /*lt, */value ->
            print("set to $value")
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
            print("set to $value")
        }
        lifetime.add { property.set(true) }
        lifetimeDef.terminate()
    }
}