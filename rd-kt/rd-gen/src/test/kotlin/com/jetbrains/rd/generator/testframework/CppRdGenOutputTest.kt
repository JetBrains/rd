package com.jetbrains.rd.generator.testframework

import com.jetbrains.rd.generator.nova.FlowTransform
import com.jetbrains.rd.generator.nova.IGenerator
import com.jetbrains.rd.generator.nova.cpp.Cpp17Generator
import java.io.File
import java.nio.file.Paths

abstract class CppRdGenOutputTest : RdGenOutputTestBase() {
    companion object {
        protected fun cppGeneratedSourcesDir(testName: String) = "build/$testName/testOutputCpp/"
        private fun cppAsIsGeneratedSourcesDir(testName: String) = "${cppGeneratedSourcesDir(testName)}/asis"
        private fun cppReversedGeneratedSourcesDir(testName: String) = "${cppGeneratedSourcesDir(testName)}/reversed"

        fun generators(testName: String, namespace: String): Array<IGenerator> = arrayOf(
            Cpp17Generator(FlowTransform.AsIs, namespace, File(cppAsIsGeneratedSourcesDir(testName))),
            Cpp17Generator(FlowTransform.Reversed, namespace, File(cppReversedGeneratedSourcesDir(testName))),
        )
    }

    override val fileExtensionNoDot = "cpp"
    override val generatedSourcesDir
        get() = cppGeneratedSourcesDir(testName)

    override fun enumerateGeneratedFiles(transform: String, model: Class<*>): List<File> {
        val transformGeneratedFilesDir = Paths.get(generatedSourcesDir, transform, model.simpleName)
        return transformGeneratedFilesDir.toFile().walk().filter { it.isFile }.toList()
    }

    override fun getGoldFile(transform: String, model: Class<*>, generatedFile: File): File {
        val goldFileName = generatedFile.name.replace(".Generated.", ".")
        val goldFileRelativePath = "testData/$testName/$transform/${model.simpleName}/$goldFileName"
        return getGoldFile(goldFileRelativePath)
    }
}
