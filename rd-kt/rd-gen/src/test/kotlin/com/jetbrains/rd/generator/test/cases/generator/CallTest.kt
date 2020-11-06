package com.jetbrains.rd.generator.test.cases.generator

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.csharp.CSharp50Generator
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rd.util.reflection.scanForResourcesContaining
import com.jetbrains.rd.util.reflection.toPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File


class CallTest {
    companion object {
        const val kotlinGeneratedSourcesDir = "build/testOutputKotlin"
        const val kotlinTempOutputDir = "build/testOutputKotlinTemp"
    }

    object TestRoot1 : Root(
        Kotlin11Generator(FlowTransform.AsIs, "org.testroot1", File(kotlinGeneratedSourcesDir)),
        CSharp50Generator(FlowTransform.AsIs, "Org.TestRoot1", File("build/testOutputCSharp"))
//        Cpp17Generator(FlowTransform.AsIs, "Org.TestRoot1", File("build/testOutputCpp"))
    )

    @Suppress("unused")
    class TestRoot2 : Root()

    object Solution2 : Ext(TestRoot1) {

        init {
            call("get", PredefinedType.int, classdef("myClass") {
              set("mySet", PredefinedType.string)
            })
        }
    }

    val classloader: ClassLoader = CallTest::class.java.classLoader

    @BeforeEach
    fun cleanup() {
        File(kotlinTempOutputDir).deleteRecursively()
    }

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
        assertNotNull(compiledClassesLoader, "Failed to compile generated sources: ${rdgen.error}")

        val generatedCodeClass = compiledClassesLoader!!.loadClass("GeneratedCodeTestKt")
        val method = generatedCodeClass.getMethod("main")
        val result = method.invoke(null) as String
        assertEquals(result, "OK", result)
    }

    /**
     * Model named [testModels.testSubpackage.FooRoot] should be captured by package filter `testModels.testSubpackage`.
     */
    @Test
    fun test2() {
        generateRdModel(classloader, arrayOf("testModels.testSubpackage"), true)

        val generatedSources = File(kotlinTempOutputDir).listFiles()!!
        assertEquals(1, generatedSources.size)
        assertNotNull(generatedSources.singleOrNull { it.name == "FooRoot.Generated.kt" })
    }

    /**
     * Model named [testModels.testSubpackage.FooRoot] shouldn't be captured by package filter `testSubpackage` (even if
     * such package exists on the the top level, but has no appropriate classes).
     */
    @Test
    fun test3() {
        generateRdModel(classloader, arrayOf("testSubpackage"), true)

        val generatedSources = File(kotlinTempOutputDir).listFiles().orEmpty()
        assertEquals(0, generatedSources.size)
    }
}

