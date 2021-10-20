package com.jetbrains.rd.generator.test.cases.generator.kotlin

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.testframework.KotlinRdGenOutputTest
import org.junit.jupiter.api.Test

class DefaultNlsValuesTest : KotlinRdGenOutputTest() {
    companion object {
        private const val testName = "defaultNlsValues"
    }

    override val testName = Companion.testName

    object DefaultNlsValuesRoot : Root(*generators(testName, "DefaultNlsValuesRoot")) {
        val classModel = classdef {
            field("fff", nlsString).default("123")
            property("ppp", nlsString, "123")
        }
    }

    @Test
    fun test1() = doTest<DefaultNlsValuesRoot>(DefaultNlsValuesRoot::class.java)
}
