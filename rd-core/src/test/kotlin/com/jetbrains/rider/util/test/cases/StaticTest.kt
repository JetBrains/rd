package com.jetbrains.rider.util.test.cases

import com.jetbrains.rider.util.Static
import com.jetbrains.rider.util.use
import org.testng.annotations.Test
import kotlin.test.assertEquals

class StaticTest {

    class Foo

    @Test
    fun test1() {
        assertEquals(null, Static.peek(Foo::class))

        val foo = Foo()
        Static.use(Foo::class, foo) {
            assertEquals(foo, Static.peek(Foo::class))

            val boo = Foo()
            Static.push(Foo::class, boo)
            assertEquals(boo, Static.peek(Foo::class))

            assertEquals(foo, Static.pop(Foo::class))
        }
        assertEquals(null, Static.peek(Foo::class))
    }
}
