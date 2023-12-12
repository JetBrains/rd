package com.jetbrains.rd.generator.test.cases.generator.csharp

import com.jetbrains.rd.generator.test.cases.generator.testModels.DocumentationModelRoot
import com.jetbrains.rd.generator.testframework.CSharpRdGenOutputTest
import org.junit.jupiter.api.Test

class CSharpDocumentationModelTest : CSharpRdGenOutputTest() {

    companion object {
        const val TEST_NAME = "documentationModelTest"
    }

    override val testName = TEST_NAME

    override val verifyComments = true

    @Test
    fun test1() = doTest<DocumentationModelRoot>(DocumentationModelRoot::class.java)
}
