package com.jetbrains.rd.generator.test.cases.generator

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.cpp.Cpp17Generator
import com.jetbrains.rd.generator.nova.csharp.CSharp50Generator
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rd.util.UsedImplicitly
import com.jetbrains.rd.util.reflection.scanForResourcesContaining
import com.jetbrains.rd.util.reflection.toPath
import org.junit.Assert
import org.junit.Test
import java.io.File


class SimpleModelTest {
    companion object {
        val kotlinGeneratedSourcesDir = "build/testOutputKotlin"
    }

    object TestRoot1 : Root(
        Kotlin11Generator(FlowTransform.AsIs, "org.testroot1", File(kotlinGeneratedSourcesDir)),
        CSharp50Generator(FlowTransform.AsIs, "Org.TestRoot1", File("build/testOutputCSharp")),
        Cpp17Generator(FlowTransform.AsIs, "Org.TestRoot1", File("build/testOutputCpp"))
    )

    @UsedImplicitly
    @Suppress("unused")
    class TestRoot2 : Root()

    object Solution : Ext(TestRoot1) {

        val editor = classdef("editor") {
            field("DocumentName", PredefinedType.int)
            property("Caret", PredefinedType.int)
            field("singleLine", PredefinedType.bool).default(false)
        }

        init {
            map("editors", PredefinedType.int, editor)
        }
    }

    @UsedImplicitly
    @Suppress("unused")
    object Markup : Ext(Solution.editor) {
        init {
            property("editor", Solution.editor)
        }
    }

    val classloader: ClassLoader = SimpleModelTest::class.java.classLoader

    @Test
    fun test1() {
        generateRdModel(classloader, arrayOf("com.jetbrains.rd.generator.test.cases.generator"), true)
        val generatedCodeTestFile = classloader.getResource("GeneratedCodeTest.kt").toPath()

        val rdgen = RdGen()

        val rdFrameworkClasspath = classloader.scanForResourcesContaining("com.jetbrains.rd.framework") +
                classloader.scanForResourcesContaining("com.jetbrains.rd.util")
        rdgen.classpath *= rdFrameworkClasspath.joinToString(File.pathSeparator)

        val generatedSources = File(kotlinGeneratedSourcesDir).walk().toList() + listOf(generatedCodeTestFile)
        val compiledClassesLoader = rdgen.compileDsl(generatedSources)
        Assert.assertNotNull("Failed to compile generated sources: ${rdgen.error}", compiledClassesLoader)

        val generatedCodeClass = compiledClassesLoader!!.loadClass("GeneratedCodeTestKt")
        val method = generatedCodeClass.getMethod("main")
        val result = method.invoke(null) as String
        Assert.assertEquals(result, "OK", result)
    }
}

