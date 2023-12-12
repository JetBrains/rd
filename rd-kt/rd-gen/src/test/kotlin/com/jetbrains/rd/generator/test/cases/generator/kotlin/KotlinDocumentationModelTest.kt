package com.jetbrains.rd.generator.test.cases.generator.kotlin

import com.jetbrains.rd.generator.test.cases.generator.testModels.DocumentationModelRoot
import com.jetbrains.rd.generator.testframework.KotlinRdGenOutputTest
import org.junit.jupiter.api.Test

class KotlinDocumentationModelTest : KotlinRdGenOutputTest() {

    companion object {
        const val TEST_NAME = "documentationModelTest"
    }

    override val testName = TEST_NAME

    override val verifyComments = true

    @Test
    fun test1() = doTest<DocumentationModelRoot>(DocumentationModelRoot::class.java)
}
