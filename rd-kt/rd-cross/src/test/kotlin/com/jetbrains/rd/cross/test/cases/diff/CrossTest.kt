package com.jetbrains.rd.cross.test.cases.diff

import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import java.io.File
import java.nio.file.Paths

class CrossTest {
    private fun assertEqualFiles(file1: File, file2: File) {
        val t1 = file1.readText(Charsets.UTF_8)
        val t2 = file2.readText(Charsets.UTF_8)
        if (t1 == t2) {
            println("The files $file1 and $file2 are same!")
        } else {
            fail("The files $file1 and $file2 differ!")
        }
    }

    companion object {
        val rootFolder: File = File(Paths.get("").toAbsolutePath().toString()).parentFile.parentFile

    }

    @After
    fun tearDown() {
//        tmpFolder.deleteRecursively()
//        tmpFolder.mkdirs()
    }

    private fun doTest(testName: String) {
        val tmpFolder = File(File(rootFolder, "build/src/main/resources/tmp"), testName)
        val goldFolder = File(File(rootFolder, "buildSrc/src/main/resources/gold"), testName)

        assertTrue("Tmp directory($tmpFolder) was not created", tmpFolder.exists())
        assertTrue("Gold directory($goldFolder) was not created", goldFolder.exists())
        goldFolder.listFiles()!!.forEach {
            val candidate = File(tmpFolder, it.nameWithoutExtension + ".tmp")
            assertTrue("File $candidate doesn't exist", candidate.exists())
            assertEqualFiles(it, candidate)
        }

        tmpFolder.listFiles()!!.forEach {
            val candidate = File(goldFolder, it.nameWithoutExtension + ".gold")
            assertTrue("Extra tmp file=$it", candidate.exists())
        }
    }

    @get:Rule
    public var name = TestName()

    private val methodName get() = name.methodName.replace("test", "")

    //region AllEntities
    @Ignore
    @Test
    fun testCrossTest_AllEntities_KtServer_CppClient() {
        doTest(methodName)
    }

    @Test
    fun testCrossTest_AllEntities_KtServer_CsClient() {
        doTest(methodName)
    }

    @Test
    fun testCrossTest_AllEntities_CsServer_KtClient() {
        doTest(methodName)
    }
    //endregion

    //region BigBuffer
    @Ignore
    @Test
    fun testCrossTest_BigBuffer_KtServer_CppClient() {
        doTest(methodName)
    }

    @Test
    fun testCrossTest_BigBuffer_KtServer_CsClient() {
        doTest(methodName)
    }
    //endregion

    //region RdCall
    @Ignore
    @Test
    fun testCrossTest_RdCall_KtServer_CppClient() {
        doTest(methodName)
    }

    @Test
    fun testCrossTest_RdCall_KtServer_CsClient() {
        doTest(methodName)
    }
    //endregion
}
