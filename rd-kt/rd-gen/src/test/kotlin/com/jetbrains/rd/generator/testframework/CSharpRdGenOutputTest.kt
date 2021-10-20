package com.jetbrains.rd.generator.testframework

import com.jetbrains.rd.generator.nova.FlowTransform
import com.jetbrains.rd.generator.nova.IGenerator
import com.jetbrains.rd.generator.nova.csharp.CSharp50Generator
import java.io.File

abstract class CSharpRdGenOutputTest : RdGenOutputTestBase() {
    companion object {
        protected fun csGeneratedSourcesDir(testName: String) = "build/$testName/testOutputCs/"
        private fun csAsIsGeneratedSourcesDir(testName: String) = "${csGeneratedSourcesDir(testName)}/asis"
        private fun csReversedGeneratedSourcesDir(testName: String) = "${csGeneratedSourcesDir(testName)}/reversed"

        fun generators(testName: String, namespace: String): Array<IGenerator> = arrayOf(
            CSharp50Generator(FlowTransform.AsIs, namespace, File(csAsIsGeneratedSourcesDir(testName))),
            CSharp50Generator(FlowTransform.Reversed, namespace, File(csReversedGeneratedSourcesDir(testName))),
        )
    }

    override val fileExtensionNoDot = "cs"
    override val generatedSourcesDir
        get() = csGeneratedSourcesDir(testName)

    override fun processLines(lines: List<String>) =
        lines.filter { !it.startsWith("//") && !it.startsWith("  ///") }
}