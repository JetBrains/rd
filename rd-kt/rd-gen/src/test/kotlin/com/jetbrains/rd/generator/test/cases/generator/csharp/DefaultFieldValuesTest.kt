package com.jetbrains.rd.generator.test.cases.generator.csharp

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.PredefinedType.int
import com.jetbrains.rd.generator.nova.PredefinedType.string
import com.jetbrains.rd.generator.testframework.CSharpRdGenOutputTest
import org.junit.jupiter.api.Test

class DefaultFieldValuesTest : CSharpRdGenOutputTest() {
    companion object {
        private const val testName = "inheritsAutomation"
    }

    override val testName = Companion.testName

    @Suppress("unused")
    object DefaultFieldValuesRoot : Root(*generators(testName, "DefaultFieldValuesRoot")) {
        val testStruct = structdef {
            field("sHasNoDefaultValue", int)
            field("sHasDefaultValue", int).default(0L)
            field("sOptional", int.nullable).optional
            field("sHasNoDefaultValueEither", int)
            field("sHasDefaultValueToo", string).default("too")
        }
        val testClass = classdef {
            field("cHasNoDefaultValue", int)
            field("cHasDefaultValue", int).default(0L)
            field("cOptional", int.nullable).optional
            field("cHasNoDefaultValueEither", int)
            field("cHasDefaultValueToo", string).default("too")

            map("foo", int, int)
        }
    }

    @Test
    fun test() = doTest<DefaultFieldValuesRoot>(
        DefaultFieldValuesRoot::class.java
    )
}