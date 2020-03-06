package com.jetbrains.rd.generator.test.cases.util

import com.jetbrains.rd.util.hash.PersistentHash
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PersistentHashTest {

    var file : Path = Paths.get("fake")

    @BeforeEach
    fun setUp() {
        file  = Files.createTempFile("PersistentHash", ".txt")
    }

    @AfterEach
    fun tearDown() {
        file.toFile().delete()
    }

    @Test
    fun testSmoke() {
        val hash = PersistentHash()
        hash.mix("a", "1")
        hash.mix("a", "2")
        hash.mix("b", "1")

        assertStoreLoad(hash)
    }

    @Test
    fun testNoKeys() {
        val hash = PersistentHash()

        assertStoreLoad(hash)
    }

    @Test
    fun testEmpty() {
        val hash = PersistentHash()
        assertEquals(PersistentHash(), hash)

        hash.mix("", "")
        assertNotEquals(PersistentHash(), hash)

        assertStoreLoad(hash)
    }

    @Test
    fun testSameValue() {
        val hash1 = PersistentHash()
        hash1.mix("a", "")
        hash1.mix("a", "")

        val hash2 = PersistentHash()
        hash2.mix("a", "")

        assertEquals(hash1, hash2)

        hash1.mix("", "b")
        assertNotEquals(hash1, hash2)

        hash2.mix("", "b")
        assertEquals(hash1, hash2)

        assertStoreLoad(hash1)
    }

    @Test
    fun testOrder() {
        val hash = PersistentHash()
        hash.mix("b", "")
        hash.mix("a", "b")

        assertStoreLoad(hash)
    }


    @Test
    fun testSpecialSymbols() {
        val hash = PersistentHash()
        hash.mix("=", ",")
        hash.mix(",", "\n\n\r\n")
        hash.mix("\\=", "\n\\")
        hash.mix("\\=", "abc")
        hash.mix("\\=\n", "abc;,=\n")

        assertStoreLoad(hash)

    }

    private fun assertStoreLoad(hash: PersistentHash) {
        hash.store(file)
        val loaded = PersistentHash.load(file)

        assertEquals(hash, loaded)
    }

    @Test
    fun testFuck() {
        val x = PersistentHash()
        x.mixFileRecursively(File("testSources"))
        x.store(file)
    }
}