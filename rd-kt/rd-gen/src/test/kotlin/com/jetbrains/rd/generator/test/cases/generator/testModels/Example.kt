package com.jetbrains.rd.generator.test.cases.generator.testModels

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.test.cases.generator.cpp.CppExampleModelTest
import com.jetbrains.rd.generator.test.cases.generator.csharp.CSharpExampleModelTest
import com.jetbrains.rd.generator.test.cases.generator.kotlin.KotlinExampleModelTest
import com.jetbrains.rd.generator.testframework.CSharpRdGenOutputTest
import com.jetbrains.rd.generator.testframework.CppRdGenOutputTest
import com.jetbrains.rd.generator.testframework.KotlinRdGenOutputTest

val outputKotlinDir = "build/exampleModelGenerated/testOutputKotlin"

object ExampleRootNova : Root(
    *KotlinRdGenOutputTest.generators(KotlinExampleModelTest.testName, "org.example"),
    *CppRdGenOutputTest.generators(CppExampleModelTest.testName, "org.example"),
    *CSharpRdGenOutputTest.generators(CSharpExampleModelTest.testName, "org.example")
)

@Suppress("unused")
object ExampleModelNova : Ext(ExampleRootNova) {
    //separately declared type
    private val selection = structdef {
        field("start", PredefinedType.int)
        field("end", PredefinedType.int)
        field("lst", array(PredefinedType.int))
        field("enumSetTest", flags {
            +"a"
            +"b"
            +"c"
        })
        field("nls_field", nlsString)
    }

    val foo = baseclass {
        field("x", PredefinedType.int)
        map("sdf", PredefinedType.int, PredefinedType.int)
    }

    private val scalarFoo = basestruct("scalarPrimer") {
        field("x", PredefinedType.int)
    }

    val Bar = baseclass("a") extends foo {
        property("y", PredefinedType.string)
        property("z", enum("z") {
            +"Bar"
            +"z1"
        })
    }

    val baz = classdef extends Bar {
        field("foo", immutableList(foo))
        field("bar", immutableList(Bar.nullable))
        field("nls_field", nlsString)
        field("nls_nullable_field", PredefinedType.string.nullable.attrs(KnownAttrs.Nls))
        field("string_list_field", immutableList(PredefinedType.string))
        field("nls_list_field", immutableList(nlsString))
        property("foo1", foo.nullable)
        property("bar1", Bar.nullable)
        map("mapScalar", PredefinedType.int, scalarFoo).readonly.async
        map("mapBindable", PredefinedType.int, FooBar)
        const("const_nls", nlsString, "const_nls_value")

        val ccNls = const("const_for_default_nls", nlsString, "291")
        property("property_with_default_nls", ccNls, KnownAttrs.Nls)

        property("property_with_several_attrs", PredefinedType.string.attrs(KnownAttrs.Nls, KnownAttrs.NonNls))
        property("nls_prop", nlsString)
        property("nullable_nls_prop", PredefinedType.string.nullable.attrs(KnownAttrs.Nls))
        field("non_nls_open_field", nonNlsString)

        property("duration_prop", PredefinedType.timeSpan)
    }

    val FooBar: Class.Concrete = classdef {
        field("a", baz)
    }

    /// Inheritance
    val Interface = interfacedef {
    }

    val Interface2 = interfacedef {
    }

    val Class = classdef {
    }

    val Struct = structdef {
    }

    val BaseClass = baseclass {
        field("baseField", PredefinedType.int)
    }

    val BaseStruct = basestruct {
        field("baseField", PredefinedType.int)
    }

    val OpenClass = openclass {
        field("baseField", PredefinedType.int)
    }

    val OpenStruct = openstruct {
        field("baseField", PredefinedType.int)
    }

    val DerivedClass = classdef extends BaseClass {
        field("derivedField", PredefinedType.bool)
    }

    val DerivedStruct = structdef extends BaseStruct {
        field("derivedField", PredefinedType.bool)
    }

    val DerivedOpenClass = openclass extends OpenClass {
        field("derivedField", PredefinedType.bool)
    }

    val DerivedOpenStruct = openstruct extends OpenStruct {
        field("derivedField", PredefinedType.bool)
    }

    val DerivedBaseClass = baseclass extends BaseClass {
        field("derivedField", PredefinedType.bool)
    }

    val DerivedBaseStruct = basestruct extends BaseStruct {
        field("derivedField", PredefinedType.bool)
    }

    val BaseClassWithInterface = baseclass implements Interface with {
    }

    val BaseStructWithInterface = basestruct implements Interface with {
    }

    val DerivedClassWith2Interfaces = baseclass extends BaseClass implements Interface implements Interface2 with {
        field("derivedField", PredefinedType.bool)
    }

    val DerivedStructWith2Interfaces = structdef extends BaseStruct implements Interface implements Interface2 with {
        field("derivedField", PredefinedType.bool)
    }

    init {
        source("push", PredefinedType.int)
        property("version", PredefinedType.int).readonly

        map("documents",
            PredefinedType.int,
            classdef("Document") {
                field("moniker", FooBar)
                field("buffer", PredefinedType.string.nullable)
                callback("andBackAgain", PredefinedType.string, PredefinedType.int)

                field("completion", classdef("Completion") {
                    map("lookupItems", PredefinedType.int, PredefinedType.bool)
                })
                field("arr1", array(PredefinedType.byte))
                field("arr2", array(array(PredefinedType.bool)))
            }
        )

        map("editors",
            structdef("ScalarExample") {
                field("intfield", PredefinedType.int)
            },
            classdef("TextControl") {
                property("selection", selection).readonly
                sink("vsink", PredefinedType.void)
                source("vsource", PredefinedType.void)  //hi vsauce, Michael here
                call("there1", PredefinedType.int, PredefinedType.string)
            }
        )
    }
}