package com.jetbrains.rd.framework.test.cases

import com.jetbrains.rd.framework.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RdIdTest {
    companion object {
        private const val HIGH_BIT = 1L shl 63
    }

    @Test
    fun testMix() {
        val id1 = RdIdUtil.mix(RdIdUtil.mix(RdId.Null, "abcd"), "efg")
        val id2 = RdIdUtil.mix(RdId.Null, "abcdefg")
        assertEquals(id1.hash, id2.hash)
        assertEquals(id1.hash, 88988021860L)//Platform Independent Hash
        assertEquals(-5123855772550266649L, RdIdUtil.mix(id2, "hijklmn").hash)
        println("InternScopeInExt".getPlatformIndependentHash())
    }

    @Test
    fun testSequentialIdentitiesNextHasNoHighBit() {
        val clientIdentities = SequentialIdentities(IdKind.Client)
        val serverIdentities = SequentialIdentities(IdKind.Server)

        repeat(1000) {
            val clientId = clientIdentities.next(RdId.Null)
            val serverId = serverIdentities.next(RdId.Null)

            assertFalse(clientId.hash and HIGH_BIT != 0L, "Client dynamic ID should not have high bit set: ${clientId.hash}")
            assertFalse(serverId.hash and HIGH_BIT != 0L, "Server dynamic ID should not have high bit set: ${serverId.hash}")
        }
    }

    @Test
    fun testSequentialIdentitiesMixAlwaysHasHighBit() {
        val clientIdentities = SequentialIdentities(IdKind.Client)
        val serverIdentities = SequentialIdentities(IdKind.Server)

        val testStrings = listOf("", "a", "test", "Protocol", "Extension", "InternRoot")
        val testInts = listOf(0, 1, -1, Int.MAX_VALUE, Int.MIN_VALUE)
        val testLongs = listOf(0L, 1L, -1L, Long.MAX_VALUE, Long.MIN_VALUE)

        for (s in testStrings) {
            val clientId = clientIdentities.mix(RdId.Null, s)
            val serverId = serverIdentities.mix(RdId.Null, s)
            assertTrue(clientId.hash and HIGH_BIT != 0L, "Client stable ID from string '$s' should have high bit set")
            assertTrue(serverId.hash and HIGH_BIT != 0L, "Server stable ID from string '$s' should have high bit set")
        }

        for (i in testInts) {
            val clientId = clientIdentities.mix(RdId.Null, i)
            val serverId = serverIdentities.mix(RdId.Null, i)
            assertTrue(clientId.hash and HIGH_BIT != 0L, "Client stable ID from int $i should have high bit set")
            assertTrue(serverId.hash and HIGH_BIT != 0L, "Server stable ID from int $i should have high bit set")
        }

        for (l in testLongs) {
            val clientId = clientIdentities.mix(RdId.Null, l)
            val serverId = serverIdentities.mix(RdId.Null, l)
            assertTrue(clientId.hash and HIGH_BIT != 0L, "Client stable ID from long $l should have high bit set")
            assertTrue(serverId.hash and HIGH_BIT != 0L, "Server stable ID from long $l should have high bit set")
        }
    }

    @Test
    fun testSequentialIdentitiesNoOverlapBetweenNextAndMix() {
        val identities = SequentialIdentities(IdKind.Client)

        val dynamicIds = mutableSetOf<Long>()
        val stableIds = mutableSetOf<Long>()

        repeat(1000) {
            dynamicIds.add(identities.next(RdId.Null).hash)
        }

        for (i in 0 until 1000) {
            stableIds.add(identities.mix(RdId.Null, "key$i").hash)
        }

        val overlap = dynamicIds.intersect(stableIds)
        assertTrue(overlap.isEmpty(), "Dynamic and stable IDs should never overlap, but found: $overlap")
    }

    @Test
    fun testSequentialIdentitiesNextIgnoresParent() {
        val identities1 = SequentialIdentities(IdKind.Client)
        val identities2 = SequentialIdentities(IdKind.Client)

        val differentParents = listOf(
            RdId.Null,
            RdId(1),
            RdId(12345),
            RdId(Long.MAX_VALUE),
            RdId(-1)
        )

        // Get IDs using different parents from identities1
        val idsWithDifferentParents = differentParents.map { parent ->
            identities1.next(parent).hash
        }

        // Get IDs using same parent from identities2
        val idsWithSameParent = differentParents.map {
            identities2.next(RdId.Null).hash
        }

        // Both should produce the same sequence since parent is ignored
        assertEquals(idsWithDifferentParents, idsWithSameParent,
            "next() should return sequential IDs regardless of parent")

        // Verify they are sequential (incrementing by 2)
        for (i in 1 until idsWithDifferentParents.size) {
            assertEquals(idsWithDifferentParents[i] - idsWithDifferentParents[i-1], 2L,
                "next() should increment by 2")
        }
    }
}
