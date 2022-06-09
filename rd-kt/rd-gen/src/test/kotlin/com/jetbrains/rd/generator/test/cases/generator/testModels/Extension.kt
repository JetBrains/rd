package com.jetbrains.rd.generator.test.cases.generator.testModels

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.csharp.CSharp50Generator
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rd.generator.test.cases.generator.csharp.CSharpExtensionTest
import com.jetbrains.rd.generator.test.cases.generator.kotlin.KotlinExtensionTest
import com.jetbrains.rd.generator.testframework.CSharpRdGenOutputTest
import com.jetbrains.rd.generator.testframework.KotlinRdGenOutputTest

object ExtensionRoot : Root(
    *KotlinRdGenOutputTest.generators(KotlinExtensionTest.testName, "ExtensionRoot"),
    *CSharpRdGenOutputTest.generators(CSharpExtensionTest.testName, "ExtensionRoot")
) {

    private fun Class.ext1(name: String) = append(Extension1(name))
    private fun Struct.ext1(name: String) = append(Extension1(name))
    private fun Class.ext2(name: String) = append(Extension2(name))
    private fun Struct.ext2(name: String) = append(Extension2(name))
    private fun Class.ext3(name: String) = append(Extension3(name))
    private fun Struct.ext3(name: String) = append(Extension3(name))

    val classWithStr = classdef {
        ext1("extFromClass1")
        ext2("extFromClass2")
        ext3("extFromClass3")
        field("reallyStrFromClass", nlsString)
    }
    val structWithStr = structdef {
        ext1("extFromStruct1")
        ext2("extFromStruct2")
        ext3("extFromStruct3")
        field("reallyStrFromStruct", nlsString)
    }

    class Extension1(name: String) : Member.Reactive.Stateful.Extension(name, PredefinedType.string.nullable,
        ExtensionDelegate(
            Kotlin11Generator::class, FlowTransform.AsIs,
            PredefinedType.string.nullable.attrs(KnownAttrs.Nls),
            null
        ),
        ExtensionDelegate(
            Kotlin11Generator::class, FlowTransform.Reversed,
            PredefinedType.string.nullable.attrs(KnownAttrs.Nls),
            null),
        ExtensionDelegate(
            CSharp50Generator::class, FlowTransform.Reversed,
            PredefinedType.string.nullable,
            null),
        ExtensionDelegate(
            CSharp50Generator::class, FlowTransform.AsIs,
            PredefinedType.string.nullable,
            null)
    )

    class Extension2(name: String) : Member.Reactive.Stateful.Extension(name, PredefinedType.string.nullable,
        ExtensionDelegate(
            Kotlin11Generator::class, FlowTransform.AsIs,
            PredefinedType.string,
            "com.jetbrains.rd.generator.test.cases.generator.testModels.fqn"
        ),

        ExtensionDelegate(
            Kotlin11Generator::class, FlowTransform.Reversed,
            PredefinedType.string,
            "com.jetbrains.rd.generator.test.cases.generator.testModels.fqn"
        ),

        ExtensionDelegate(
            CSharp50Generator::class, FlowTransform.Reversed,
            PredefinedType.int,
            "Int.Parse"),

        ExtensionDelegate(
            CSharp50Generator::class, FlowTransform.AsIs,
            PredefinedType.int,
            "int.Parse")
    )

    class Extension3(name: String) : Member.Reactive.Stateful.Extension(name, PredefinedType.string.nullable,
        ExtensionDelegate(
            Kotlin11Generator::class, FlowTransform.AsIs,
            PredefinedType.string.nullable,
            null
        ),
        ExtensionDelegate(
            Kotlin11Generator::class, FlowTransform.Reversed,
            PredefinedType.string.nullable,
            null),
        ExtensionDelegate(
            CSharp50Generator::class, FlowTransform.Reversed,
            "LocalizedString",
            "SomeFactory"),
        ExtensionDelegate(
            CSharp50Generator::class, FlowTransform.AsIs,
            "LocalizedString",
            "SomeFactory")
    )
}

fun fqn(str: String?) = str!!