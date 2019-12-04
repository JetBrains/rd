package com.jetbrains.rd.cross.test.cases.diff

import com.jetbrains.rd.util.eol
import org.junit.*
import org.junit.Assert.assertTrue
import org.junit.rules.TestName
import java.io.File
import java.nio.file.Paths

class CrossTest {
    private fun File.toHighlightablePath() = "file:///" + this.invariantSeparatorsPath

    private fun assertEqualFiles(expectedFile: File, actualFile: File) {
        val expectedText = expectedFile.readText(Charsets.UTF_8)
        val actualText = actualFile.readText(Charsets.UTF_8)
        if (expectedText == actualText) {
            println("The files ${expectedFile.toHighlightablePath()} and ${actualFile.toHighlightablePath()} are same!")
        } else {
            createUpdateGoldScript(expectedFile, actualFile)
            throw ComparisonFailure("The files ${expectedFile.toHighlightablePath()} and ${actualFile.toHighlightablePath()} differ!$eol", expectedText, actualText)
        }
    }

    private fun createUpdateGoldScript(expectedFile: File, actualFile: File) {
        val dir = createTempDir("JetTestScripts")
        dir.mkdir()
        val baseName = "copyToGold_${actualFile.nameWithoutExtension}"
        var file = File(dir, "$baseName.bat")
        var index = 1
        while (file.exists()) {
            file = File(dir, "$baseName$index.bat")
            index++
        }
        file.createNewFile()
        file.writeText("move /y \"${actualFile.absolutePath}\" \"${expectedFile.absolutePath}\"")
        println("To update gold file use this script: ${file.toHighlightablePath()}")
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
    var name = TestName()

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
