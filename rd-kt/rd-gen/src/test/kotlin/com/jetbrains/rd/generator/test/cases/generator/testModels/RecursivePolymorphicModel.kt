package com.jetbrains.rd.generator.test.cases.generator.testModels

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.test.cases.generator.csharp.RecursivePolymorphicCSharpTest
import com.jetbrains.rd.generator.test.cases.generator.kotlin.RecursivePolymorphicKotlinTest
import com.jetbrains.rd.generator.testframework.CSharpRdGenOutputTest
import com.jetbrains.rd.generator.testframework.KotlinRdGenOutputTest

object RecursivePolymorphicModelRoot : Root(
    *KotlinRdGenOutputTest.generators(RecursivePolymorphicKotlinTest.testName, "org.example"),
    *CSharpRdGenOutputTest.generators(RecursivePolymorphicCSharpTest.testName, "org.example")
)

object RecursivePolymorphicModel : Ext(RecursivePolymorphicModelRoot) {
    internal val BeTreeGridLine = openclass BeTreeGridLine@ {
        list("children", this@BeTreeGridLine)
    }

    init {
        property("line", BeTreeGridLine)
        property("list", immutableList(BeTreeGridLine))
    }
}