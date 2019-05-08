package com.jetbrains.rd.generator.test.cases.generator.example

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.PredefinedType.*
import com.jetbrains.rd.generator.nova.cpp.Cpp17Generator
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rd.generator.nova.util.syspropertyOrEmpty
import java.io.File

object ExampleRootNova : Root(
        Kotlin11Generator(FlowTransform.AsIs, "org.example", File(syspropertyOrEmpty("model.out.src.kt.dir"))),
        Cpp17Generator(FlowTransform.AsIs, "org.example", File(syspropertyOrEmpty("model.out.src.cpp.dir")))
//    CSharp50Generator(FlowTransform.Reversed, "org.example", File("C:/work/Rider/Platform/RdProtocol/rider-generated/Src//com/jetbrains/rider/model.cSharp"), "[ShellComponent]")
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
        property("foo1", foo.nullable)
        property("bar1", Bar.nullable)
        map("mapScalar", int, scalarFoo).readonly.async
        map("mapBindable", int, FooBar)
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
    //    @Test
    fun test() {
        val pkg1 = javaClass.`package`.name.apply { println(this) }
        val pkg2 = "com.jetbrains.rd.modeltemplate"
        val classloader = javaClass.classLoader
        generateRdModel(classloader, arrayOf(pkg1, pkg2), true)
    }
}
