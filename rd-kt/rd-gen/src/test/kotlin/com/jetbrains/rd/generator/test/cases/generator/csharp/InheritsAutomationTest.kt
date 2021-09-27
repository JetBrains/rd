package com.jetbrains.rd.generator.test.cases.generator.csharp

import com.jetbrains.rd.generator.nova.Ext
import com.jetbrains.rd.generator.nova.Root
import com.jetbrains.rd.generator.nova.csharp.CSharp50Generator
import com.jetbrains.rd.generator.nova.setting
import com.jetbrains.rd.generator.testframework.CSharpRdGenOutputTest
import org.junit.jupiter.api.Test

class InheritsAutomationTest : CSharpRdGenOutputTest() {
    companion object {
        private const val testName = "inheritsAutomation"
    }

    override val testName = Companion.testName

    object InheritsAutomationRoot : Root(*generators(testName, "InheritsAutomationRoot"))
    object InheritsAutomationExtension : Ext(InheritsAutomationRoot) {
        val TestModel =  baseclass { setting(CSharp50Generator.InheritsAutomation, true) }
    }

    @Test
    fun test() = doTest<InheritsAutomationRoot>(
        InheritsAutomationRoot::class.java,
        InheritsAutomationExtension::class.java
    )
}