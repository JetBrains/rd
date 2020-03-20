package com.jetbrains.rd.cross.test.cases.diff

import com.jetbrains.rd.util.eol
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.extension.ExtendWith
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
            assertEquals(expectedText, actualText) { "The files ${expectedFile.toHighlightablePath()} and ${actualFile.toHighlightablePath()} differ!$eol" }
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

    @BeforeEach
    fun init(testInfo: TestInfo) {
        testName = testInfo.displayName
    }

    @AfterEach
    fun tearDown() {
//        tmpFolder.deleteRecursively()
//        tmpFolder.mkdirs()
    }

    private fun doTest(testName: String) {
        val tmpFolder = File(File(rootFolder, "build/src/main/resources/tmp"), testName)
        val goldFolder = File(File(rootFolder, "buildSrc/src/main/resources/gold"), testName)

        assertTrue(tmpFolder.exists()) { "Tmp directory($tmpFolder) was not created" }
        assertTrue(goldFolder.exists()) { "Gold directory($goldFolder) was not created" }
        goldFolder.listFiles()!!.forEach {
            val candidate = File(tmpFolder, it.nameWithoutExtension + ".tmp")
            assertTrue(candidate.exists()) { "File $candidate doesn't exist" }
            assertEqualFiles(it, candidate)
        }

        tmpFolder.listFiles()!!.forEach {
            val candidate = File(goldFolder, it.nameWithoutExtension + ".gold")
            assertTrue(candidate.exists()) { "Extra tmp file=$it" }
        }
    }

    @get:ExtendWith
    lateinit var testName: String

    private val methodName
        get() = testName
            .replace("test", "")
            .dropLast(2)

    //region AllEntities
    @Disabled
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
    @Disabled
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
    @Disabled
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
