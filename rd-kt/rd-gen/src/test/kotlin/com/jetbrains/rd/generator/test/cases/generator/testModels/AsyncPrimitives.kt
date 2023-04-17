package com.jetbrains.rd.generator.test.cases.generator.testModels

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.test.cases.generator.kotlin.AsyncPrimitivesTest
import com.jetbrains.rd.generator.testframework.CSharpRdGenOutputTest
import com.jetbrains.rd.generator.testframework.KotlinRdGenOutputTest

object AsyncPrimitivesRoot : Root(
    *KotlinRdGenOutputTest.generators(AsyncPrimitivesTest.testName, "org.example"),
    *CSharpRdGenOutputTest.generators(AsyncPrimitivesTest.testName, "org.example")
)

object AsyncPrimitivesExt : Ext(AsyncPrimitivesRoot) {
    init {
        asyncProperty("asyncProperty", PredefinedType.string)
        asyncProperty("asyncPropertyNullable", PredefinedType.string.nullable)
    }
}

