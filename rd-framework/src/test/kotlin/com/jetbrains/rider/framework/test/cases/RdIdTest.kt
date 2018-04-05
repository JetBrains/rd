package com.jetbrains.rider.framework.test.cases

import com.jetbrains.rider.framework.RdId
import kotlin.test.Test
import kotlin.test.assertEquals

class RdIdTest {
    @Test
    fun testMix() {
        val id1 = RdId.Null.mix("abcd").mix("efg")
        val id2 = RdId.Null.mix("abcdefg")
        assertEquals(id1.hash, id2.hash)
    }
}
