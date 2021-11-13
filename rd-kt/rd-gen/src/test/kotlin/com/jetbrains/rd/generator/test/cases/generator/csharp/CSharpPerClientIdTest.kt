package com.jetbrains.rd.generator.test.cases.generator.csharp

import com.jetbrains.rd.generator.test.cases.generator.testModels.PerClientIdRoot
import com.jetbrains.rd.generator.testframework.CSharpRdGenOutputTest
import org.junit.jupiter.api.Test

class CSharpPerClientIdTest : CSharpRdGenOutputTest() {
    companion object {
        const val testName = "perClientId"
    }

    override val testName = Companion.testName
    @Test
    fun test1() = doTest<PerClientIdRoot>(PerClientIdRoot::class.java)
}
