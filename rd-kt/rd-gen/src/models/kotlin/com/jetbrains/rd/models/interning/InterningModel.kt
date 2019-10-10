package com.jetbrains.rd.models.interning

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.cpp.Cpp17Generator
import com.jetbrains.rd.generator.nova.csharp.CSharp50Generator
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rd.generator.nova.util.syspropertyOrInvalid
import java.io.File

@Suppress("unused")
object InterningRoot1 : Root(
        Kotlin11Generator(FlowTransform.AsIs, "com.jetbrains.rd.framework.test.cases.interning", File(syspropertyOrInvalid("model.out.src.kt.dir"))),
        CSharp50Generator(FlowTransform.AsIs, "JetBrains.Platform.Tests.Cases.RdFramework.Interning", File("build/testOutputCSharp")),
        Cpp17Generator(FlowTransform.AsIs, "rd.test.util", File(syspropertyOrInvalid("model.out.src.cpp.dir")), true)
) {
    init {
        setting(Cpp17Generator.TargetName, "interning_test_model")
//        setting(Kotlin11Generator.MasterStateful, false)
//        setting(CSharp50Generator.MasterStateful, false)
        setting(Cpp17Generator.MasterStateful, false)
    }
    val TestInternScope = /*InterningModelsGenTest.InterningRoot1.*/internScope()

    val InterningTestModel = /*InterningModelsGenTest.InterningRoot1.*/classdef {
        internRoot(TestInternScope)

        field("searchLabel", PredefinedType.string)
        map("issues", PredefinedType.int, /*InterningModelsGenTest.InterningRoot1.*/structdef("WrappedStringModel") {
            field("text", PredefinedType.string.interned(TestInternScope))
        })
    }

    val InterningNestedTestModel = /*InterningModelsGenTest.InterningRoot1.*/structdef {
        field("value", PredefinedType.string)
        field("inner", this.interned(TestInternScope).nullable)
    }

    val InterningNestedTestStringModel = /*InterningModelsGenTest.InterningRoot1.*/structdef {
        field("value", PredefinedType.string.interned(TestInternScope))
        field("inner", this.nullable)
    }

    val InterningProtocolLevelModel = /*InterningModelsGenTest.InterningRoot1.*/classdef {
        field("searchLabel", PredefinedType.string)
        map("issues", PredefinedType.int, /*InterningModelsGenTest.InterningRoot1.*/structdef("ProtocolWrappedStringModel") {
            field("text", PredefinedType.string.interned(ProtocolInternScope))
        })
    }

    val InterningMtModel = /*InterningModelsGenTest.InterningRoot1.*/classdef {
        internRoot(TestInternScope)

        field("searchLabel", PredefinedType.string)
        signal("signaller", PredefinedType.string.interned(TestInternScope)).async
        signal("signaller2", PredefinedType.string.interned(TestInternScope)).async
    }

    val InternScopeOutOfExt = /*InterningModelsGenTest.InterningRoot1.*/internScope()
    val InterningExtensionHolder = /*InterningModelsGenTest.InterningRoot1.*/classdef {
        internRoot(InternScopeOutOfExt)
    }


}

@Suppress("Unused")
object InterningExt : Ext(InterningRoot1.InterningExtensionHolder) {
    val InternScopeInExt = /*InterningModelsGenTest.InterningExt.*/internScope()

    init {
        property("root", /*InterningModelsGenTest.InterningExt.*/classdef("InterningExtRootModel") {
            internRoot(InternScopeInExt)

            property("internedLocally", PredefinedType.string.interned(InternScopeInExt))
            property("internedExternally", PredefinedType.string.interned(InterningRoot1.InternScopeOutOfExt))
            property("internedInProtocol", PredefinedType.string.interned(ProtocolInternScope))
        })
    }
}