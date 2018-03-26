package com.jetbrains.rider.framework.test.cases

import com.jetbrains.rider.framework.RdId
import org.testng.Assert
import org.testng.annotations.Test

class RdIdTest {
    @Test
    fun testMix() {
        val id1 = RdId.Null.mix("abcd").mix("efg")
        val id2 = RdId.Null.mix("abcdefg")
        Assert.assertEquals(id1.hash, id2.hash)
    }
}
