package com.jetbrains.rd.generator.test.cases.generator

import com.jetbrains.rd.generator.nova.generateRdModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class ModelPackageSearchTests {
    companion object {
        const val kotlinTempOutputDir = "build/testOutputKotlinTemp"
    }

    val classloader: ClassLoader = ModelPackageSearchTests::class.java.classLoader
    @BeforeEach
    fun cleanup() {
        File(kotlinTempOutputDir).deleteRecursively()
    }

    /**
     * Model named [testModels.testSubpackage.FooRoot] should be captured by package filter `testModels.testSubpackage`.
     */
    @Test
    fun test1() {
        generateRdModel(classloader, arrayOf("testModels.testSubpackage"), true)

        val generatedSources = File(kotlinTempOutputDir).listFiles()!!
        assertEquals(1, generatedSources.size)
        assertNotNull(generatedSources.singleOrNull { it.name == "FooRoot.Generated.kt" })
    }

    /**
     * Model named [testModels.testSubpackage.FooRoot] shouldn't be captured by package filter `testSubpackage` (even if
     * such package exists on the the top level, but has no appropriate classes).
     */
    @Test
    fun test2() {
        generateRdModel(classloader, arrayOf("testSubpackage"), true)

        val generatedSources = File(kotlinTempOutputDir).listFiles().orEmpty()
        assertEquals(0, generatedSources.size)
    }
}
