package com.jetbrains.rd.generator.test.cases.generator.testModels

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.test.cases.generator.cpp.CppDocumentationModelTest
import com.jetbrains.rd.generator.test.cases.generator.csharp.CSharpDocumentationModelTest
import com.jetbrains.rd.generator.test.cases.generator.kotlin.KotlinDocumentationModelTest
import com.jetbrains.rd.generator.testframework.CSharpRdGenOutputTest
import com.jetbrains.rd.generator.testframework.CppRdGenOutputTest
import com.jetbrains.rd.generator.testframework.KotlinRdGenOutputTest

object DocumentationModelRoot : Root(
    *KotlinRdGenOutputTest.generators(KotlinDocumentationModelTest.TEST_NAME, "org.example"),
    *CppRdGenOutputTest.generators(CppDocumentationModelTest.TEST_NAME, "org.example"),
    *CSharpRdGenOutputTest.generators(CSharpDocumentationModelTest.TEST_NAME, "org.example")
) {
    init {
        documentation = "This is a documentation test,\nand it is also multiline."
    }
}
