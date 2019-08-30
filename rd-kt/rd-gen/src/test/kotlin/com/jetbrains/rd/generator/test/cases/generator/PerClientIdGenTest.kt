package com.jetbrains.rd.generator.test.cases.generator

import com.jetbrains.rd.framework.FrameworkMarshallers
import com.jetbrains.rd.framework.IMarshaller
import com.jetbrains.rd.framework.ISerializer
import com.jetbrains.rd.framework.impl.RdMap
import com.jetbrains.rd.framework.impl.RdProperty
import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.cpp.Cpp17Generator
import com.jetbrains.rd.generator.nova.csharp.CSharp50Generator
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rd.util.reflection.scanForResourcesContaining
import org.junit.Assert
import org.junit.Test
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
        init {
            property("aProp", PredefinedType.string).perClientId
            map("aMap", PredefinedType.string, PredefinedType.string).perClientId

            property("innerProp", classdef("InnerClass") {
                property("someValue", PredefinedType.string.nullable).perClientId
                property("someClassValue", structdef("PerClientIdStruct") {}).perClientId
                signal("someClassSignal", structdef("PerClientIdSignal") {}).perClientId
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
        Assert.assertNotNull("Failed to compile generated sources: ${rdgen.error}", compiledClassesLoader)
    }
}

