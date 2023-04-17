package com.jetbrains.rd.generator.test.cases.generator.kotlin

import com.jetbrains.rd.generator.nova.Root
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rd.generator.nova.setting
import com.jetbrains.rd.generator.testframework.KotlinRdGenOutputTest
import org.junit.jupiter.api.Test

class CustomThreadingTest : KotlinRdGenOutputTest() {
    companion object {
        const val testName = "customThreading"
    }

    override val testName = Companion.testName
    object CustomThreadingRoot : Root(*generators(testName, "CustomThreadingRoot")) {

        init {
            setting(Kotlin11Generator.ThreadingKind, Kotlin11Generator.ExtThreadingKind.CustomScheduler)
        }
    }

    @Test
    fun test1() = doTest<CustomThreadingRoot>(CustomThreadingRoot::class.java)
}