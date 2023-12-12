package com.jetbrains.rd.generator.test.cases.generator.cpp

import com.jetbrains.rd.generator.test.cases.generator.testModels.DocumentationModelRoot
import com.jetbrains.rd.generator.testframework.CppRdGenOutputTest
import org.junit.jupiter.api.Test

class CppDocumentationModelTest : CppRdGenOutputTest() {

    companion object {
        const val TEST_NAME = "documentationModelTest"
    }

    override val testName = TEST_NAME

    override val verifyComments = true

    override fun expectedFileCount(model: Class<*>) = when (model) {
        DocumentationModelRoot::class.java -> 2
        else -> 0
    }

    @Test
    fun test1() = doTest<DocumentationModelRoot>(DocumentationModelRoot::class.java)
}
