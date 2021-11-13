package com.jetbrains.rd.generator.test.cases.generator.kotlin

import com.jetbrains.rd.generator.test.cases.generator.testModels.ExampleModelNova
import com.jetbrains.rd.generator.test.cases.generator.testModels.ExampleRootNova
import com.jetbrains.rd.generator.testframework.KotlinRdGenOutputTest
import org.junit.jupiter.api.Test

class ExampleModelTest : KotlinRdGenOutputTest() {
    companion object {
        const val testName = "exampleModelTest"
    }

    override val testName = Companion.testName

    @Test
    fun test1() = doTest<ExampleRootNova>(ExampleRootNova::class.java, ExampleModelNova::class.java)
}
