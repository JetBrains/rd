package com.jetbrains.rd.cross.test.cases.diff

import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.nio.file.Paths

class CrossTest {
    private fun assertEqualFiles(file1: File, file2: File) {
        val t1 = file1.readText(Charsets.UTF_8)
        val t2 = file2.readText(Charsets.UTF_8)
        if (t1 == t2) {
            println("The files $file1 and $file2 are same!")
        } else {
            fail(
                """
                    |The files $file1 and $file2 differ!
                    |$file1:
                    |$t1
                    |$file2:
                    |$t2
                """.trimMargin()
            )
        }
    }

    companion object {
        val rootFolder = File(Paths.get("").toAbsolutePath().toString()).parentFile
        val tmpFolder = File(rootFolder, "build/src/main/resources/tmp")
    }

    @After
    fun tearDown() {
        tmpFolder.deleteRecursively()
        tmpFolder.mkdirs()
    }

    @Test
    fun testAll() {
        val goldSubFolder = System.getProperty("CrossTestName")!!
        val goldFolder = File(File(rootFolder, "buildSrc/src/main/resources/gold"), goldSubFolder)

        assertTrue("Tmp directory($tmpFolder) was not created", tmpFolder.exists())
        assertTrue("Gold directory($goldFolder) was not created", goldFolder.exists())
        goldFolder.listFiles()!!.forEach {
            val candidate = File(tmpFolder, it.nameWithoutExtension + ".tmp")
            assertTrue("File $candidate doesn't exist", candidate.exists())
            assertEqualFiles(it, candidate)
        }

        tmpFolder.listFiles()!!.forEach {
            val candidate = File(goldFolder, it.nameWithoutExtension + ".gold")
            assertTrue  ("Extra tmp file=$it", candidate.exists())
        }
    }
}
