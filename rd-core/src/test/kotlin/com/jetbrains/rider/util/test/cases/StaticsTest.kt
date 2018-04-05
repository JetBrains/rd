package com.jetbrains.rider.util.test.cases

import com.jetbrains.rider.util.Statics
import kotlin.test.assertEquals
import kotlin.test.Test

class StaticsTest {

    class Foo

    @Test
    fun test1() {
        val statics = Statics<Foo>()
        assertEquals(null, statics.get())

        val foo = Foo()
        statics.use(foo) {
            assertEquals(foo, statics.get())

            val boo = Foo()
            val toDispose = statics.push(boo)
            assertEquals(boo, statics.get())

            toDispose.close()
            assertEquals(foo, statics.get())
        }
        assertEquals(null, statics.get())
    }
}
