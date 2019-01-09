package com.jetbrains.rd.framework.test.cases

import com.jetbrains.rd.framework.RdId
import kotlin.test.Test
import kotlin.test.assertEquals

class RdIdTest {
    @Test
    fun testMix() {
        val id1 = RdId.Null.mix("abcd").mix("efg")
        val id2 = RdId.Null.mix("abcdefg")
        assertEquals(id1.hash, id2.hash)
        assertEquals(id1.hash, 88988021860L)//Platform Independent Hash
    }
}
