package com.jetbrains.rd.generator.test.cases.generator.kotlin

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.testframework.KotlinRdGenOutputTest
import com.jetbrains.rd.util.reflection.toPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class CallGeneratedCodeTest : KotlinRdGenOutputTest() {
    companion object {
        private const val testName = "callGeneratedCode"
    }

    override val testName = Companion.testName

    object CallGeneratedCodeRoot : Root(*generators(testName, "call.generated.code.root")) {
        val editor = classdef("editor") {
            field("DocumentName", PredefinedType.int)
            property("Caret", PredefinedType.int)
            field("singleLine", PredefinedType.bool).default(false)

            field("es", flags("es") {
                +"a"
                +"b"
                +"c"
            }).default("")

            field("xxx", immutableList(structdef("Abc"){

            }.interned(ProtocolInternScope)).nullable).optional
        }
    }

    override fun customGeneratedSources(): List<File> {
        val classLoader = CallGeneratedCodeTest::class.java.classLoader
        return listOf(classLoader.getResource("GeneratedCodeTest.kt")!!.toPath())
    }

    @Test
    fun test1() {
        doTest<CallGeneratedCodeRoot>(CallGeneratedCodeRoot::class.java) { compiledClassLoader ->
            val generatedCodeClass = compiledClassLoader.loadClass("GeneratedCodeTestKt")
            val method = generatedCodeClass.getMethod("main")
            val result = method.invoke(null) as String
            assertEquals(result, "OK", result)
        }
    }
}

