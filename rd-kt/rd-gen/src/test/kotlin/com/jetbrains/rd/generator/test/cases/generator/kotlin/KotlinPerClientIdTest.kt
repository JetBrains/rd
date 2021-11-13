package com.jetbrains.rd.generator.test.cases.generator.kotlin

import com.jetbrains.rd.generator.test.cases.generator.testModels.PerClientIdRoot
import com.jetbrains.rd.generator.testframework.KotlinRdGenOutputTest
import org.junit.jupiter.api.Test

class KotlinPerClientIdTest : KotlinRdGenOutputTest() {
    companion object {
        const val testName = "perClientId"
    }

    override val testName = Companion.testName
    @Test
    fun test1() = doTest<PerClientIdRoot>(PerClientIdRoot::class.java)
}
