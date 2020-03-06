package com.jetbrains.rd.generator.test.cases.generator

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.csharp.CSharp50Generator
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rd.util.reflection.scanForResourcesContaining
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.io.File


class PerClientIdGenTest {
    companion object {
        val kotlinGeneratedSourcesDir = "build/testOutputKotlin"
    }

    @Suppress("unused")
    object PerClientIdRoot1 : Root(
            Kotlin11Generator(FlowTransform.AsIs, "com.jetbrains.rd.framework.test.cases.perClientId", File(kotlinGeneratedSourcesDir)),
            CSharp50Generator(FlowTransform.AsIs, "JetBrains.Platform.Tests.Cases.RdFramework.PerClientId", File("build/testOutputCSharp"))
    ) {

        val key = context(PredefinedType.string)
        val lightKey = context(PredefinedType.int).light
        init {
            property("aProp", PredefinedType.string).perContext(key)
            property("aPropDefault", false).perContext(key)
            property("aPropDefault2", true)
            map("aMap", PredefinedType.string, PredefinedType.string).perContext(key)

            property("innerProp", classdef("InnerClass") {
                property("someValue", PredefinedType.string.nullable).perContext(key)
                property("someClassValue", structdef("PerClientIdStruct") {}).perContext(key)
                signal("someClassSignal", structdef("PerClientIdSignal") {}).perContext(key)
            })
        }
    }

    val classloader: ClassLoader = PerClientIdGenTest::class.java.classLoader

    @Test
    fun test1() {
        val files = generateRdModel(classloader, arrayOf("com.jetbrains.rd.generator.test.cases.generator"), true)
        assert(!files.isEmpty()) { "No files generated, bug?" }

        val rdgen = RdGen()

        val rdFrameworkClasspath = classloader.scanForResourcesContaining("com.jetbrains.rd.framework") +
                classloader.scanForResourcesContaining("com.jetbrains.rd.util")
        rdgen.classpath *= rdFrameworkClasspath.joinToString(File.pathSeparator)

        val generatedSources = File(kotlinGeneratedSourcesDir).walk().toList()
        val compiledClassesLoader = rdgen.compileDsl(generatedSources)
        assertNotNull(compiledClassesLoader, "Failed to compile generated sources: ${rdgen.error}")
    }
}

