package com.jetbrains.rd.generator.test.cases.generator.csharp

import com.jetbrains.rd.generator.test.cases.generator.testModels.ExtensionRoot
import com.jetbrains.rd.generator.testframework.CSharpRdGenOutputTest
import org.junit.jupiter.api.Test

class CSharpExtensionTest : CSharpRdGenOutputTest() {

    companion object {
        const val testName = "extensionTest"
    }

    override val testName = Companion.testName
    @Test
    fun test1() = doTest<ExtensionRoot>(ExtensionRoot::class.java)
}