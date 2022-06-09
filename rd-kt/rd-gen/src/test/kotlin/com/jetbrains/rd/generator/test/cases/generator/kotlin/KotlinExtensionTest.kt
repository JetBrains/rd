package com.jetbrains.rd.generator.test.cases.generator.kotlin

import com.jetbrains.rd.generator.test.cases.generator.testModels.ExtensionRoot
import com.jetbrains.rd.generator.testframework.KotlinRdGenOutputTest
import org.junit.jupiter.api.Test

class KotlinExtensionTest : KotlinRdGenOutputTest() {

    override val testName = Companion.testName

    companion object {
        internal const val testName = "extensionTest"
    }


    @Test
    fun test1() = doTest<ExtensionRoot>(ExtensionRoot::class.java)

}