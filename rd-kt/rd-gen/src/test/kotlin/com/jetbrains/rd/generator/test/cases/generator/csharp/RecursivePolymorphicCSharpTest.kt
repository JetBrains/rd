package com.jetbrains.rd.generator.test.cases.generator.csharp

import com.jetbrains.rd.generator.test.cases.generator.testModels.RecursivePolymorphicModel
import com.jetbrains.rd.generator.test.cases.generator.testModels.RecursivePolymorphicModelRoot
import com.jetbrains.rd.generator.testframework.CSharpRdGenOutputTest
import com.jetbrains.rd.generator.testframework.KotlinRdGenOutputTest
import org.junit.jupiter.api.Test

class RecursivePolymorphicCSharpTest : CSharpRdGenOutputTest() {
    companion object {
        const val testName = "recursivePolymorphicModelTest"
    }

    override val testName = Companion.testName

    @Test
    fun test1() = doTest<RecursivePolymorphicModelRoot>(RecursivePolymorphicModelRoot::class.java, RecursivePolymorphicModel::class.java)
}