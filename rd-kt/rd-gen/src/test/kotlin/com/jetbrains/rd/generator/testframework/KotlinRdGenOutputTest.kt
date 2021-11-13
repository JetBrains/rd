package com.jetbrains.rd.generator.testframework

import com.jetbrains.rd.generator.nova.FlowTransform
import com.jetbrains.rd.generator.nova.IGenerator
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import java.io.File

abstract class KotlinRdGenOutputTest : RdGenOutputTestBase() {
    companion object {
        protected fun ktGeneratedSourcesDir(testName: String) = "build/$testName/testOutputKt"
        private fun ktAsIsGeneratedSourcesDir(testName: String) = "${ktGeneratedSourcesDir(testName)}/asis"
        private fun ktReversedGeneratedSourcesDir(testName: String) = "${ktGeneratedSourcesDir(testName)}/reversed"

        fun generators(testName: String, namespace: String): Array<IGenerator> = arrayOf(
            Kotlin11Generator(FlowTransform.AsIs, namespace, File(ktAsIsGeneratedSourcesDir(testName))),
            Kotlin11Generator(FlowTransform.Reversed, namespace, File(ktReversedGeneratedSourcesDir(testName)))
        )
    }

    override val compileAfterGenerate = true
    override val fileExtensionNoDot = "kt"
    override val generatedSourcesDir
        get() = ktGeneratedSourcesDir(testName)
}
