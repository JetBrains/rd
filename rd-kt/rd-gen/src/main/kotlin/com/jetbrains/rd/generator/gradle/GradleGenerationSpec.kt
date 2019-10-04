package com.jetbrains.rd.generator.gradle

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.cpp.Cpp17Generator
import com.jetbrains.rd.generator.nova.csharp.CSharp50Generator
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import java.io.File

data class GradleGenerationSpec(
        var language: String = "",
        var transform: String? = null,
        var root: String = "",
        var namespace: String = "",
        var directory: String = ""
) {

    fun toGeneratorAndRoot(availableRoots: List<Root>) : IGeneratorAndRoot {
            val flowTransform = when (transform) {
                "asis" -> FlowTransform.AsIs
                "reversed" -> FlowTransform.Reversed
                "symmetric" -> FlowTransform.Symmetric
                null -> FlowTransform.AsIs
                else -> throw GeneratorException("Unknown flow transform type ${transform}, use 'asis', 'reversed' or 'symmetric'")
            }
            val generator = when (language) {
                "kotlin" -> Kotlin11Generator(flowTransform, namespace, File(directory))
                "csharp" -> CSharp50Generator(flowTransform, namespace, File(directory))
                "cpp" -> Cpp17Generator(flowTransform, namespace, File(directory))
                else -> throw GeneratorException("Unknown language $language, use 'kotlin' or 'csharp' or 'cpp'")
            }

            val root = availableRoots.find { it.javaClass.canonicalName == root }
                    ?: throw GeneratorException("Can't find root with class name ${root}. Found roots: " +
                            availableRoots.joinToString { it.javaClass.canonicalName })

            return ExternalGenerator(generator, root)
    }
}