package com.jetbrains.rd.generator.test.cases.generator.example

import com.jetbrains.rd.generator.nova.RdGen
import com.jetbrains.rd.generator.nova.generateRdModel
import com.jetbrains.rd.generator.test.cases.generator.testModels.ExampleModelNova
import com.jetbrains.rd.generator.test.cases.generator.testModels.outputKotlinDir
import com.jetbrains.rd.util.reflection.scanForResourcesContaining
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

class TestExample {

    //    @Test
    fun test() {
        val pkg1 = javaClass.`package`.name.apply { println(this) }
        val pkg2 = "com.jetbrains.rd.modeltemplate"
        val classloader = javaClass.classLoader
        generateRdModel(classloader, arrayOf(pkg1, pkg2), true)
    }


    @Test
    fun test1() {
        val classloader: ClassLoader = ExampleModelNova::class.java.classLoader

        val files = generateRdModel(classloader, arrayOf("com.jetbrains.rd.generator.test.cases.generator.testModels"), true)
        assert(files.isNotEmpty()) { "No files generated, bug?" }

        val rdgen = RdGen().apply { verbose *= true }

        val rdFrameworkClasspath = classloader.scanForResourcesContaining("com.jetbrains.rd.framework") +
            classloader.scanForResourcesContaining("com.jetbrains.rd.util") +
            classloader.scanForResourcesContaining("org.jetbrains.annotations")

        rdgen.verbose *= true
        rdgen.classpath *= rdFrameworkClasspath.joinToString(File.pathSeparator)

        val generatedSources = File(outputKotlinDir).walk().toList()
        val compiledClassesLoader = rdgen.compileDsl(generatedSources)
        Assertions.assertNotNull(compiledClassesLoader, "Failed to compile generated sources: ${rdgen.error}")
    }
}
