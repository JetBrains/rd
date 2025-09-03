package com.jetbrains.rd.generator.test.cases.generator.cpp

import com.jetbrains.rd.generator.test.cases.generator.testModels.ExampleModelNova
import com.jetbrains.rd.generator.test.cases.generator.testModels.ExampleRootNova
import com.jetbrains.rd.generator.testframework.CppRdGenOutputTest
import org.junit.jupiter.api.Test

class CppExampleModelTest : CppRdGenOutputTest() {
    companion object {
        const val testName = "exampleModelTest"
    }

    override val testName = Companion.testName

    override fun expectedFileCount(model: Class<*>) = when (model) {
        ExampleRootNova::class.java -> 2
        ExampleModelNova::class.java -> 94
        else -> 0
    }

    @Test
    fun test1() = doTest<ExampleRootNova>(ExampleRootNova::class.java, ExampleModelNova::class.java)
}
