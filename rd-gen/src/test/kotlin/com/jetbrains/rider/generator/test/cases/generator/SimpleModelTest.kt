package com.jetbrains.rider.generator.test.cases.generator

import com.jetbrains.rider.generator.nova.*
import com.jetbrains.rider.generator.nova.csharp.CSharp50Generator
import com.jetbrains.rider.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rider.util.UsedImplicitly
import org.junit.Test
import java.io.File


class SimpleModelTest {

    object TestRoot1 : Root(
        Kotlin11Generator(FlowTransform.AsIs, "org.testroot1", File("c:/temp/testOutput/testroot1")),
        CSharp50Generator(FlowTransform.AsIs, "Org.TestRoot1", File("c:/temp/testOutput/testroot1"))
    )

    @UsedImplicitly
    @Suppress("unused")
    class TestRoot2 : Root()

    object Solution : Ext(TestRoot1) {

        val editor = classdef("editor") {
            field("DocumentName", PredefinedType.int)
            property("Caret", PredefinedType.int)
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
         generateRdModel(classloader, arrayOf("com.jetbrains.rider.framework.test.cases.generator"), true)
    }
}

