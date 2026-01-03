package com.jetbrains.rd.generator.testframework

import com.jetbrains.rd.generator.nova.generateRdModel
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

    protected open val verifyComments = false

    protected open fun expectedFileCount(model: Class<*>): Int = 1

    protected inline fun <reified TModel> doTest(
        vararg models: Class<*>
    ) {
        val classLoader = TModel::class.java.classLoader
        val containingPackage = TModel::class.java.`package`.name
        val transformations = listOf("asis", "reversed")

        val files = generateRdModel(classLoader, arrayOf(containingPackage), true)
        assert(files.isNotEmpty()) { "No files generated!" }

        for (transform in transformations) {
            for (model in models) {
                val generatedFiles = enumerateGeneratedFiles(transform, model)
                Assertions.assertEquals(expectedFileCount(model), generatedFiles.size, "file count for model $model")

                for (generatedFile in generatedFiles) {
                    val goldFile = getGoldFile(transform, model, generatedFile)

                    val createGoldVar = System.getenv("CREATE_GOLD") ?: ""
                    if (createGoldVar.equals("true", ignoreCase = true) || createGoldVar == "1") {
                        generatedFile.copyTo(goldFile, overwrite = true)
                    }

                    val goldText = processText(goldFile.readLines())
                    val generatedText = processText(generatedFile.readLines())

                    Assertions.assertEquals(
                        goldText,
                        generatedText,
                        "Generated and gold sources should be the same for model class ${model.simpleName}, transformation $transform"
                    )
                }
            }
        }
    }

    protected open fun enumerateGeneratedFiles(transform: String, model: Class<*>): List<File> {
        val transformGeneratedFilesDir = Paths.get(generatedSourcesDir, transform)
        return listOf(transformGeneratedFilesDir.resolve("${model.simpleName}.Generated.$fileExtensionNoDot").toFile())
    }

    protected open fun getGoldFile(transform: String, model: Class<*>, generatedFile: File): File {
        val goldFileRelativePath = "testData/$testName/$transform/${model.simpleName}.$fileExtensionNoDot"
        return getGoldFile(goldFileRelativePath)
    }

    protected open fun customGeneratedSources(): List<File> = emptyList()

    @BeforeEach
    fun cleanup() {
        testFolder.deleteRecursively()
    }

    private fun processLines(lines: List<String>): Sequence<String> {
        val result = lines.asSequence().map { it.trimEnd() }
        return if (verifyComments) result
        else result.filter { !it.trim().startsWith("//") }
    }
    protected fun processText(lines: List<String>) = processLines(lines).joinToString("\n")

    protected fun getGoldFile(resourceRelativePath: String): File {
        val testDirectory = File(".").canonicalFile
        var currentDirectory = testDirectory
        while (currentDirectory.name != "rd-gen") {
            val parent = currentDirectory.parentFile
                ?: throw Exception("Wasn't able to find parent directory \"rd-gen\" from \"$testDirectory\".")

            currentDirectory = parent
        }

        val rdGen = currentDirectory
        return rdGen.resolve("src/test/resources").resolve(resourceRelativePath)
    }
}