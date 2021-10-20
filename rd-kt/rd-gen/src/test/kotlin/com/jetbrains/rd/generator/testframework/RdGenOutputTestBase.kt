package com.jetbrains.rd.generator.testframework

import com.jetbrains.rd.generator.nova.generateRdModel
import com.jetbrains.rd.util.reflection.toPath
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import java.io.File
import java.nio.file.Paths

abstract class RdGenOutputTestBase {

    protected abstract val testName: String
    private val testFolder
        get() = File("build/$testName")

    protected abstract val fileExtensionNoDot: String
    protected abstract val generatedSourcesDir: String

    protected inline fun <reified TModel> doTest(vararg models: Class<*>) {
        val classLoader = TModel::class.java.classLoader
        val containingPackage = TModel::class.java.`package`.name
        val transformations = listOf("asis", "reversed")

        generateRdModel(classLoader, arrayOf(containingPackage), true)

        for (transform in transformations) {
            for (model in models) {
                val goldFileResourcePath = "testData/$testName/$transform/${model.simpleName}.$fileExtensionNoDot"
                val goldFile = classLoader.getResource(goldFileResourcePath)?.toPath()
                Assertions.assertNotNull(goldFile, "Resource $goldFileResourcePath should exist")
                val generatedFile = Paths.get(generatedSourcesDir, transform, "${model.simpleName}.Generated.$fileExtensionNoDot").toFile()

                val goldText = processText(goldFile!!.readLines())
                val generatedText = processText(generatedFile.readLines())

                Assertions.assertEquals(
                    goldText,
                    generatedText,
                    "Generated and gold sources should be the same for model class ${model.simpleName}, transformation $transform"
                )
            }
        }
    }

    @BeforeEach
    fun cleanup() {
        testFolder.deleteRecursively()
    }

    protected open fun processLines(lines: List<String>) = lines.map { it.trimEnd() }
    protected fun processText(lines: List<String>) = processLines(lines).joinToString("\n")
}