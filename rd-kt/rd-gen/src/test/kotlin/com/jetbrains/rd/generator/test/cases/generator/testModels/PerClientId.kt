package com.jetbrains.rd.generator.test.cases.generator.testModels

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.test.cases.generator.csharp.CSharpPerClientIdTest
import com.jetbrains.rd.generator.test.cases.generator.kotlin.KotlinPerClientIdTest
import com.jetbrains.rd.generator.testframework.CSharpRdGenOutputTest
import com.jetbrains.rd.generator.testframework.KotlinRdGenOutputTest

object PerClientIdRoot : Root(
    *KotlinRdGenOutputTest.generators(KotlinPerClientIdTest.testName, "com.jetbrains.rd.framework.test.cases.perClientId"),
    *CSharpRdGenOutputTest.generators(CSharpPerClientIdTest.testName, "JetBrains.Platform.Tests.Cases.RdFramework.PerClientId")
) {
    private val key = threadLocalContext(PredefinedType.string)
    val lightKey = threadLocalContext(PredefinedType.int).light
    init {
        property("aProp", PredefinedType.string).perContext(key)
        property("aPropDefault", false).perContext(key)
        property("aPropDefault2", true)
        map("aMap", PredefinedType.string, PredefinedType.string).perContext(key)

        property("innerProp", classdef("InnerClass") {
            property("someValue", PredefinedType.string.nullable).perContext(key)
            property("someClassValue", structdef("PerClientIdStruct") {}).perContext(key)
            signal("someClassSignal", structdef("PerClientIdSignal") {}).perContext(key)
        })
    }
}
