package com.jetbrains.rd.generator.test.cases.generator.example

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.PredefinedType.*
import com.jetbrains.rd.generator.nova.cpp.Cpp17Generator
import com.jetbrains.rd.generator.nova.csharp.CSharp50Generator
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rd.util.reflection.scanForResourcesContaining
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

object ExampleRootNova : Root(
        Kotlin11Generator(FlowTransform.AsIs, "org.example", File("build/exampleModelGenerated/testOutputKotlin")),
        Cpp17Generator(FlowTransform.AsIs, "org.example", File("build/exampleModelGenerated/testOutputCpp")),
        CSharp50Generator(FlowTransform.AsIs, "org.example", File("build/exampleModelGenerated/testOutputCSharp"))
)

@Suppress("unused")
object ExampleModelNova : Ext(ExampleRootNova) {
    //separately declared type
    private val selection = structdef {
        field("start", int)
        field("end", int)
        field("lst", array(int))
        field("enumSetTest", flags {
            +"a"
            +"b"
            +"c"
        })
        field("nls_field", nlsString)
    }

    val foo = baseclass {
        field("x", int)
        map("sdf", int, int)
    }

    private val scalarFoo = basestruct("scalarPrimer") {
        field("x", int)
    }

    val Bar = baseclass("a") extends foo {
        property("y", string)
        property("z", enum("z") {
            +"Bar"
            +"z1"
        })
    }

    val baz = classdef extends Bar {
        field("foo", immutableList(foo))
        field("bar", immutableList(Bar.nullable))
        field("nls_field", nlsString)
        field("nls_nullable_field", string.nullable.attrs(KnownAttrs.Nls))
        field("string_list_field", immutableList(string))
        field("nls_list_field", immutableList(nlsString))
        property("foo1", foo.nullable)
        property("bar1", Bar.nullable)
        map("mapScalar", int, scalarFoo).readonly.async
        map("mapBindable", int, FooBar)
        const("const_nls", nlsString, "const_nls_value")

        val ccNls = const("const_for_default_nls", nlsString, "291")
        property("property_with_default_nls", ccNls, KnownAttrs.Nls)

        property("property_with_several_attrs", string.attrs(KnownAttrs.Nls, KnownAttrs.NonNls))
        property("nls_prop", nlsString)
        property("nullable_nls_prop", string.nullable.attrs(KnownAttrs.Nls))
        field("non_nls_open_field", nonNlsString)
    }

    val FooBar: Class.Concrete = classdef {
        field("a", baz)
    }


    init {
        source("push", int)
        property("version", int).readonly

        map("documents",
                int,
                classdef("Document") {
                    field("moniker", FooBar)
                    field("buffer", string.nullable)
                    callback("andBackAgain", string, int)

                    field("completion", classdef("Completion") {
                        map("lookupItems", int, bool)
                    })
                    field("arr1", array(byte))
                    field("arr2", array(array(bool)))
                }
        )

        map("editors",
                structdef("ScalarExample") {
                    field("intfield", int)
                },
                classdef("TextControl") {
                    property("selection", selection).readonly
                    sink("vsink", void)
                    source("vsource", void)  //hi vsauce, Michael here
                    call("there1", int, string)
                }
        )
    }
}

class TestExample {

    companion object {
        val kotlinGeneratedSourcesDir = "build/testOutputKotlin"
    }

    //    @Test
    fun test() {
        val pkg1 = javaClass.`package`.name.apply { println(this) }
        val pkg2 = "com.jetbrains.rd.modeltemplate"
        val classloader = javaClass.classLoader
        generateRdModel(classloader, arrayOf(pkg1, pkg2), true)
    }


    @Test
    fun test1() {
        val classloader: ClassLoader = ExampleModelNova::class.java.classLoader

        val files = generateRdModel(classloader, arrayOf("com.jetbrains.rd.generator.test.cases.generator.example"), true)
        assert(files.isNotEmpty()) { "No files generated, bug?" }

        val rdgen = RdGen()

        val rdFrameworkClasspath = classloader.scanForResourcesContaining("com.jetbrains.rd.framework") +
            classloader.scanForResourcesContaining("com.jetbrains.rd.util") +
            classloader.scanForResourcesContaining("org.jetbrains.annotations")

        rdgen.verbose *= true
        rdgen.classpath *= rdFrameworkClasspath.joinToString(File.pathSeparator)

        val generatedSources = File(kotlinGeneratedSourcesDir).walk().toList()
        val compiledClassesLoader = rdgen.compileDsl(generatedSources)
        Assertions.assertNotNull(compiledClassesLoader, "Failed to compile generated sources: ${rdgen.error}")
    }
}
