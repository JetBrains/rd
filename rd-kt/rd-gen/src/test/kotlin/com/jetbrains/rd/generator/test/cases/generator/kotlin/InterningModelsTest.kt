package com.jetbrains.rd.generator.test.cases.generator.kotlin

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.testframework.KotlinRdGenOutputTest
import org.junit.jupiter.api.Test

class InterningModelsTest : KotlinRdGenOutputTest() {
    companion object {
        private const val testName = "interningModelsTest"
    }

    override val testName = Companion.testName

    object InternedModelsRoot : Root(*generators(testName, "InternedModelsRoot")) {
        private val editor = classdef("editor") {
            field("xxx", immutableList(structdef("Abc") {}.interned(ProtocolInternScope)).nullable).optional
        }

        init {
            map("editors", PredefinedType.int, editor)
        }
    }

    @Test
    fun test1() = doTest<InternedModelsRoot>(InternedModelsRoot::class.java)
}

