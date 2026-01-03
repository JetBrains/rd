package com.jetbrains.rd.models.interning

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.cpp.Cpp17Generator
import com.jetbrains.rd.generator.nova.csharp.CSharp50Generator
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rd.generator.paths.cppDirectorySystemPropertyKey
import com.jetbrains.rd.generator.paths.ktDirectorySystemPropertyKey
import com.jetbrains.rd.generator.paths.outputDirectory
import java.io.File

const val folder = "interning"

@Suppress("unused")
object InterningRoot1 : Root(
    Kotlin11Generator(FlowTransform.AsIs, "com.jetbrains.rd.framework.test.cases.interning", outputDirectory(ktDirectorySystemPropertyKey, folder)),
    CSharp50Generator(FlowTransform.AsIs, "JetBrains.Platform.Tests.Cases.RdFramework.Interning", File("build/testOutputCSharp")),
    Cpp17Generator(FlowTransform.AsIs, "rd::test::util", outputDirectory(cppDirectorySystemPropertyKey, folder))
) {
    init {
        setting(Cpp17Generator.TargetName, "interning_test_model")
    }

    val TestInternScope = internScope()

    val InterningTestModel = classdef {
        internRoot(TestInternScope)

        field("searchLabel", PredefinedType.string)
        map("issues", PredefinedType.int, structdef("WrappedStringModel") {
            field("text", PredefinedType.string.interned(TestInternScope))
        })
    }

    val InterningNestedTestModel = structdef {
        field("value", PredefinedType.string)
        field("inner", this.interned(TestInternScope).nullable)
    }

    val InterningNestedTestStringModel = structdef {
        field("value", PredefinedType.string.interned(TestInternScope))
        field("inner", this.nullable)
    }

    val InterningProtocolLevelModel = classdef {
        field("searchLabel", PredefinedType.string)
        map("issues", PredefinedType.int, structdef("ProtocolWrappedStringModel") {
            field("text", PredefinedType.string.interned(ProtocolInternScope))
        })
    }

    val InterningMtModel = classdef {
        internRoot(TestInternScope)

        field("searchLabel", PredefinedType.string)
        signal("signaller", PredefinedType.string.interned(TestInternScope)).async
        signal("signaller2", PredefinedType.string.interned(TestInternScope)).async
    }

    val InternScopeOutOfExt = internScope()
    val InterningExtensionHolder = classdef {
        internRoot(InternScopeOutOfExt)
    }


}

@Suppress("Unused")
object InterningExt : Ext(InterningRoot1.InterningExtensionHolder) {
    val InternScopeInExt = internScope()

    init {
        property("root", classdef("InterningExtRootModel") {
            internRoot(InternScopeInExt)

            property("internedLocally", PredefinedType.string.interned(InternScopeInExt))
            property("internedExternally", PredefinedType.string.interned(InterningRoot1.InternScopeOutOfExt))
            property("internedInProtocol", PredefinedType.string.interned(ProtocolInternScope))
        })
    }
}