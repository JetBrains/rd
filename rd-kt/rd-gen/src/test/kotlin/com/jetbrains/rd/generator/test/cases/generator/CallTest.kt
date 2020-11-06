package com.jetbrains.rd.generator.test.cases.generator

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.cpp.Cpp17Generator
import com.jetbrains.rd.generator.nova.csharp.CSharp50Generator
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rd.util.reflection.scanForResourcesContaining
import com.jetbrains.rd.util.reflection.toPath
import org.jetbrains.kotlin.fir.builder.generateAccessExpression
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.io.File


class CallTest {
    companion object {
        const val kotlinGeneratedSourcesDir = "build/testOutputKotlin"
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

    @Test
    fun test2() {
        generateRdModel(classloader, arrayOf("callTest2"), true)
        val generatedOutputPath = "build/generatedOutputCallTest"
        File(generatedOutputPath).listFiles()!!.forEach {
            it.delete()
        }

        val rdgen = RdGen()

        val rdFrameworkClasspath = classloader.scanForResourcesContaining("com.jetbrains.rd.framework") +
            classloader.scanForResourcesContaining("com.jetbrains.rd.util")
        rdgen.classpath *= rdFrameworkClasspath.joinToString(File.pathSeparator)


        rdgen.run()

        val generatedSources = File(generatedOutputPath).listFiles()!!
        assertEquals(2, generatedSources.size)
    }
}

