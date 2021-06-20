package com.jetbrains.rd.generator.test.cases.compiler

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.util.reflection.scanForResourcesContaining
import org.junit.Assert
import org.junit.jupiter.api.Test
import java.io.File


class InterningModelsGenTest {
    companion object {
        const val kotlinGeneratedSourcesDir = "build/testOutputKotlin"
    }

    val classloader: ClassLoader = InterningModelsGenTest::class.java.classLoader

    @Test
    fun test1() {
        System.setProperty("model.out.src.kt.dir", kotlinGeneratedSourcesDir)

        val files = generateRdModel(classloader, arrayOf("com.jetbrains.rd.generator.test.cases.generator"), true)
        assert(files.isNotEmpty()) { "No files generated, bug?" }

        val rdgen = RdGen().apply { verbose *= true }

        val rdFrameworkClasspath = classloader.scanForResourcesContaining("com.jetbrains.rd.framework") +
                classloader.scanForResourcesContaining("com.jetbrains.rd.util")
        rdgen.classpath *= rdFrameworkClasspath.joinToString(File.pathSeparator)

        val generatedSources = File(kotlinGeneratedSourcesDir).walk().toList()
        val compiledClassesLoader = rdgen.compileDsl(generatedSources)
        Assert.assertNotNull("Failed to compile generated sources: ${rdgen.error}", compiledClassesLoader)
    }
}

