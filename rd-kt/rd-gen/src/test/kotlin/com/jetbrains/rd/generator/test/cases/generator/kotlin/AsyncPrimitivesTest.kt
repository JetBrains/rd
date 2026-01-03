package com.jetbrains.rd.generator.test.cases.generator.kotlin

import com.jetbrains.rd.generator.test.cases.generator.testModels.AsyncPrimitivesExt
import com.jetbrains.rd.generator.test.cases.generator.testModels.AsyncPrimitivesRoot
import com.jetbrains.rd.generator.testframework.KotlinRdGenOutputTest
import org.junit.jupiter.api.Test

class AsyncPrimitivesTest : KotlinRdGenOutputTest() {
    companion object {
        const val testName = "asyncPrimitives"
    }

    override val testName = Companion.testName

    @Test
    fun test1() = doTest<AsyncPrimitivesRoot>(AsyncPrimitivesRoot::class.java, AsyncPrimitivesExt::class.java)
}