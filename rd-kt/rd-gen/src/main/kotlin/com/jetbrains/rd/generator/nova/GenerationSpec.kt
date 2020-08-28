package com.jetbrains.rd.generator.nova

import com.jetbrains.rd.generator.nova.cpp.Cpp17Generator
import com.jetbrains.rd.generator.nova.csharp.CSharp50Generator
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import java.io.File

data class GenerationSpec(
    var language: String = "",
    var transform: String? = null,
    var root: String = "",
    var namespace: String = "",
    var directory: String = "",
    var generatedFileSuffix: String = ".Generated"
) {
    companion object {
        fun loadFrom(file: File): List<GenerationSpec> {
            val result = mutableListOf<GenerationSpec>()
            val lines = file.readLines(Charsets.UTF_8)
            for (line in lines) {
                val parts = line.split("||")
                result.add(GenerationSpec(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5]))
            }
            return result
        }
    }

    fun toGeneratorAndRoot(availableRoots: List<Root>): IGeneratorAndRoot {
        val flowTransform = when (transform) {
            "asis" -> FlowTransform.AsIs
            "reversed" -> FlowTransform.Reversed
            "symmetric" -> FlowTransform.Symmetric
            null -> FlowTransform.AsIs
            else -> throw GeneratorException("Unknown flow transform type ${transform}, use 'asis', 'reversed' or 'symmetric'")
        }
        val generator = when (language) {
            "kotlin" -> Kotlin11Generator(flowTransform, namespace, File(directory), generatedFileSuffix)
            "csharp" -> CSharp50Generator(flowTransform, namespace, File(directory), generatedFileSuffix)
            "cpp" -> Cpp17Generator(flowTransform, namespace, File(directory), generatedFileSuffix)
            else -> throw GeneratorException("Unknown language $language, use 'kotlin' or 'csharp' or 'cpp'")
        }

        val root = availableRoots.find { it.javaClass.canonicalName == root }
            ?: throw GeneratorException("Can't find root with class name ${root}. Found roots: " +
                availableRoots.joinToString { it.javaClass.canonicalName })

        return ExternalGenerator(generator, root)
    }
}