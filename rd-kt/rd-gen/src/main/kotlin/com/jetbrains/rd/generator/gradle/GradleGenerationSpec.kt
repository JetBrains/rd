package com.jetbrains.rd.generator.gradle

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.cpp.Cpp17Generator
import com.jetbrains.rd.generator.nova.csharp.CSharp50Generator
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rd.util.eol
import java.io.File

data class GradleGenerationSpec(
        var language: String = "",
        var transform: String? = null,
        var root: String = "",
        var namespace: String = "",
        /**
         * One or more directories separated by ';'
         */
        var directory: String = ""
) {

    fun toGenerationUnit(availableRoots: List<Root>) : IGenerationUnit {
            val flowTransform = when (transform) {
                "asis" -> FlowTransform.AsIs
                "reversed" -> FlowTransform.Reversed
                "symmetric" -> FlowTransform.Symmetric
                null -> FlowTransform.AsIs
                else -> throw GeneratorException("Unknown flow transform type ${transform}, use 'asis', 'reversed' or 'symmetric'")
            }
            val directories = directory.split(';').map { File(it) }
            val first = directories[0]
            val rest = directories.drop(1)
            val generator = when (language) {
                    "kotlin" -> Kotlin11Generator(flowTransform, namespace, first, multipleFolders = rest)
                    "csharp" -> CSharp50Generator(flowTransform, namespace, first, multipleFolders = rest)
                    "cpp" -> Cpp17Generator(flowTransform, namespace, first, multipleFolders = rest)
                    else -> throw GeneratorException("Unknown language $language, use 'kotlin' or 'csharp' or 'cpp'")
            }

            val root = availableRoots.find { it.javaClass.canonicalName == root }
                    ?: throw GeneratorException("Can't find root with class name ${root}. Found roots: " +
                            availableRoots.joinToString(separator = eol) { it.javaClass.canonicalName })

            return GenerationUnit(generator, root)
    }
}