package com.jetbrains.rd.models.contexts

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.cpp.Cpp17Generator
import com.jetbrains.rd.generator.nova.csharp.CSharp50Generator
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rd.generator.nova.util.syspropertyOrInvalid
import java.io.File


@Suppress("unused")
object PerClientIdRoot1 : Root(
        Kotlin11Generator(FlowTransform.AsIs, "com.jetbrains.rd.framework.test.cases.contexts", File(syspropertyOrInvalid("model.out.src.kt.dir"))),
        CSharp50Generator(FlowTransform.AsIs, "JetBrains.Platform.Tests.Cases.RdFramework.Contexts", File("build/testOutputCSharp")),
        Cpp17Generator(FlowTransform.AsIs, "rd.test.util", File(syspropertyOrInvalid("model.out.src.cpp.dir")))
) {

    val key = context(PredefinedType.string)
    val lightKey = context(PredefinedType.int).light
    init {
        setting(Cpp17Generator.TargetName, "contexts_test_model")
//        setting(Kotlin11Generator.MasterStateful, false)
//        setting(CSharp50Generator.MasterStateful, false)
//        setting(Cpp17Generator.MasterStateful, false)

        property("aProp", PredefinedType.string).perContext(key)
        property("aPropDefault", false).perContext(key)
        property("aPropDefault2", true)
        map("aMap", PredefinedType.string, PredefinedType.string).perContext(key)

        property("innerProp", classdef("InnerClass") {
            property("someValue", PredefinedType.string.nullable).perContext(key)
            property("someClassValue", structdef("PerClientIdStruct") {}).perContext(key)
            signal("someClassSignal", structdef("PerClientIdSignal") {}).perContext(key)
        })

        property("aPropPerLight", false).perContext(lightKey)
    }
}
