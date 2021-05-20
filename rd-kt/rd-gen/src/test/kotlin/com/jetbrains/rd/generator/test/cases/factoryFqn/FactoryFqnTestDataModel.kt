package com.jetbrains.rd.generator.test.cases.factoryFqn

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.csharp.CSharp50Generator
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rd.util.reflection.toPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File


class FactoryFqnTestDataModel {
    companion object {
        const val testFolder = "build/factoryFqn"
        const val csGeneratedSourcesDir = "$testFolder/testOutputCs/"
        const val csAsIsGeneratedSourcesDir = "$csGeneratedSourcesDir/asis"
        const val csReversedGeneratedSourcesDir = "$csGeneratedSourcesDir/reversed"
        const val ktReversedGeneratedSourcesDir = "$testFolder/testOutputKtReversed"
        const val ktGeneratedSourcesDir = "$testFolder/testOutputKt"
    }

    object TestRoot1 : Root(
        CSharp50Generator(FlowTransform.AsIs, "Org.TestRoot1", File(csAsIsGeneratedSourcesDir)),
        CSharp50Generator(FlowTransform.Reversed, "Org.TestRoot1", File(csReversedGeneratedSourcesDir)),
        Kotlin11Generator(FlowTransform.AsIs, "Org.TestRoot1", File(ktGeneratedSourcesDir)),
        Kotlin11Generator(FlowTransform.Reversed, "Org.TestRoot1", File(ktReversedGeneratedSourcesDir))

   )

    @Suppress("unused")
    class TestRoot2 : Root()


    class RdTextBuffer(name: String) : Member.Reactive.Stateful.Extension(name, Solution2.rdTextBufferState,
        ExtensionDelegate(CSharp50Generator::class, FlowTransform.Reversed,
            "JetBrains.Rd.Text.Impl.RdTextBuffer",
            "JetBrains.Rd.Text.Impl.RdTextBuffer.CreateByFactory"
        ),
        ExtensionDelegate(CSharp50Generator::class, null,
            "JetBrains.Rd.Text.Impl.RdDeferrableTextBuffer"),

        ExtensionDelegate(Kotlin11Generator::class, FlowTransform.Reversed,
            "JetBrains.Rd.Text.Impl.RdTextBuffer",
            "JetBrains.Rd.Text.Impl.RdTextBuffer.CreateByFactory"
        ),
        ExtensionDelegate(Kotlin11Generator::class, null,
            "JetBrains.Rd.Text.Impl.RdDeferrableTextBuffer")
    )

    object Solution2 : Ext(TestRoot1) {

        val RdDocumentModel = classdef {
            append(RdTextBuffer("text"))
        }

        init {
            call("get", PredefinedType.int, classdef("myClass") {
                set("mySet", PredefinedType.string)
            })

            property("version", PredefinedType.string.nullable)
            property("testBuffer", RdDocumentModel)
        }

        val rdTextBufferState = classdef {
            setting(CSharp50Generator.Namespace, "JetBrains.Rd.Text.Impl.Intrinsics")
            setting(CSharp50Generator.Intrinsic)

            property("changes", PredefinedType.string)
        }
    }

    val classloader: ClassLoader = FactoryFqnTestDataModel::class.java.classLoader

    @BeforeEach
    fun cleanup() {
        File(testFolder).deleteRecursively()
    }

    @Test
    fun test1() {
        generateRdModel(classloader, arrayOf("com.jetbrains.rd.generator.test.cases.factoryFqn"), true)

        for (transform in arrayOf("asis", "reversed")) {
            val goldText1 = processText(
                classloader.getResource("testData/factoryFqn/$transform/Solution2.cs").toPath().readLines()
            )
            val generatedText1 = processText(File("$csGeneratedSourcesDir$transform/Solution2.Generated.cs").readLines())
            assertEquals(goldText1, generatedText1, "Solution2 is not same for transform: $transform")

            val goldText2 = processText(
                classloader.getResource("testData/factoryFqn/reversed/TestRoot1.cs").toPath().readLines()
            )
            val generatedText2 = processText(File("$csGeneratedSourcesDir$transform/TestRoot1.Generated.cs").readLines())
            assertEquals(goldText2, generatedText2, "TestRoot1 is not same for transform: $transform")
        }
    }

    fun processText(s : List<String>) = s.filter { !it.startsWith("//") && !it.startsWith("  ///") }
        .joinToString("\n") { it }
}

