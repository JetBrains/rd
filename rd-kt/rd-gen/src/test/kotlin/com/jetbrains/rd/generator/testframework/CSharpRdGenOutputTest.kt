package com.jetbrains.rd.generator.testframework

import com.jetbrains.rd.generator.nova.FlowTransform
import com.jetbrains.rd.generator.nova.IGenerator
import com.jetbrains.rd.generator.nova.csharp.CSharp50Generator
import com.jetbrains.rd.generator.nova.generateRdModel
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rd.util.reflection.toPath
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import java.io.File
import java.nio.file.Paths

abstract class CSharpRdGenOutputTest {
    companion object {
        protected fun csGeneratedSourcesDir(testName: String) = "build/$testName/testOutputCs/"
        private fun csAsIsGeneratedSourcesDir(testName: String) = "${csGeneratedSourcesDir(testName)}/asis"
        private fun csReversedGeneratedSourcesDir(testName: String) = "${csGeneratedSourcesDir(testName)}/reversed"
        private fun ktReversedGeneratedSourcesDir(testName: String) = "build/$testName/testOutputKtReversed"
        private fun ktGeneratedSourcesDir(testName: String) = "build/$testName/testOutputKt"

        fun generators(testName: String, namespace: String): Array<IGenerator> = arrayOf(
            CSharp50Generator(FlowTransform.AsIs, namespace, File(csAsIsGeneratedSourcesDir(testName))),
            CSharp50Generator(FlowTransform.Reversed, namespace, File(csReversedGeneratedSourcesDir(testName))),
            Kotlin11Generator(FlowTransform.AsIs, namespace, File(ktGeneratedSourcesDir(testName))),
            Kotlin11Generator(FlowTransform.Reversed, namespace, File(ktReversedGeneratedSourcesDir(testName)))
        )
    }

    abstract val testName: String
    private val testFolder
        get() = File("build/$testName")

    protected inline fun <reified TModel> doTest(models: List<Class<*>>) {
        val classLoader = TModel::class.java.classLoader
        val containingPackage = TModel::class.java.`package`.name
        val transformations = listOf("asis", "reversed")

        generateRdModel(classLoader, arrayOf(containingPackage), true)

        for (transform in transformations) {
            for (model in models) {
                val goldFileResourcePath = "testData/$testName/$transform/${model.simpleName}.cs"
                val goldFile = classLoader.getResource(goldFileResourcePath)?.toPath()
                Assertions.assertNotNull(goldFile, "Resource $goldFileResourcePath should exist")
                val generatedFile = Paths.get(csGeneratedSourcesDir(testName), transform, "${model.simpleName}.Generated.cs").toFile()

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

    protected fun processText(s : List<String>) = s.filter { !it.startsWith("//") && !it.startsWith("  ///") }
        .joinToString("\n") { it }
}