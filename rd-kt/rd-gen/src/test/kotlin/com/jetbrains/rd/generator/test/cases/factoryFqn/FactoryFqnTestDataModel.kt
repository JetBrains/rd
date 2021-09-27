package com.jetbrains.rd.generator.test.cases.factoryFqn

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.csharp.CSharp50Generator
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rd.generator.testframework.CSharpRdGenOutputTest
import org.junit.jupiter.api.Test

class FactoryFqnTestDataModel : CSharpRdGenOutputTest() {
    companion object {
        const val testName = "factoryFqn"
    }

    override val testName = Companion.testName

    object TestRoot1 : Root(*generators(testName, "Org.TestRoot1"))

    @Suppress("unused")
    class TestRoot2 : Root()

    class RdTextBuffer(name: String) : Member.Reactive.Stateful.Extension(name, Solution2.rdTextBufferState,
        ExtensionDelegate(CSharp50Generator::class, FlowTransform.Reversed,
            "JetBrains.Rd.Text.Impl.RdTextBuffer",
            "JetBrains.Rd.Text.Impl.RdTextBuffer.CreateByFactory"
        ),
        ExtensionDelegate(CSharp50Generator::class, null,
            "JetBrains.Rd.Text.Impl.RdDeferrableTextBuffer"),

        ExtensionDelegate(Kotlin11Generator::class, FlowTransform.Reversed,
            "JetBrains.Rd.Text.Impl.RdTextBuffer",
            "JetBrains.Rd.Text.Impl.RdTextBuffer.CreateByFactory"
        ),
        ExtensionDelegate(Kotlin11Generator::class, null,
            "JetBrains.Rd.Text.Impl.RdDeferrableTextBuffer")
    )

    object Solution2 : Ext(TestRoot1) {

        val RdDocumentModel = classdef {
            append(RdTextBuffer("text"))

            field("nls_field", nlsString)
            field("nullable_nls_field", PredefinedType.string.nullable.attrs(KnownAttrs.Nls))
            field("string_list_field", immutableList(PredefinedType.string))
            field("nls_list_field", immutableList(nlsString))
        }

        init {
            call("get", PredefinedType.int, classdef("myClass") {
                set("mySet", PredefinedType.string)
            })

            property("version", PredefinedType.string.nullable)
            property("testBuffer", RdDocumentModel)
            property("string_list_prop", immutableList(PredefinedType.string))
            property("nls_prop", nlsString)
            property("nls_list_prop", immutableList(nlsString))
            property("nullable_nls_prop", PredefinedType.string.nullable.attrs(KnownAttrs.Nls))
            property("nullable_nls_list_prop", immutableList(PredefinedType.string.nullable.attrs(KnownAttrs.Nls)))
        }

        val rdTextBufferState = classdef {
            setting(CSharp50Generator.Namespace, "JetBrains.Rd.Text.Impl.Intrinsics")
            setting(CSharp50Generator.Intrinsic)

            property("changes", PredefinedType.string)
        }
    }

    @Test
    fun test1() = doTest<FactoryFqnTestDataModel>(listOf(Solution2::class.java, TestRoot1::class.java))
}

