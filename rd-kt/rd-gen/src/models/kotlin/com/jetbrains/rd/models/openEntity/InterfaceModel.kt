package com.jetbrains.rd.models.openEntity

import com.jetbrains.rd.generator.nova.Ext
import com.jetbrains.rd.generator.nova.PredefinedType.string
import com.jetbrains.rd.generator.nova.PredefinedType.void
import com.jetbrains.rd.generator.nova.field
import com.jetbrains.rd.generator.nova.method


class InterfaceModel : Ext(OpenEntityRoot) {

    val marker1 = interfacedef{}
    val marker2 = interfacedef{}

    val a = interfacedef {}

    val testInterface = interfacedef.extends(marker1, marker2){
        method("testInterfaceMethod", void)
    }

    val derivedInterface = interfacedef extends testInterface {
        method("derivedInterfaceMethod", string, "something" to string)
        method("derivedInterfaceMethod2", string, Pair("something", string))
    }

    // base classes

    val testClass = baseclass implements testInterface with {
        field("testClassField", string)
    }

    val derivedTestClass = baseclass extends testClass implements derivedInterface + a with {
        field("derivedTestClassField", string)
    }

    val multipleInterfaceDerivedTestClass = baseclass extends testClass implements marker1 + marker2 + a with {
        field("multipleInterfaceDerivedTestClassField", string)
    }

    // open classes

    val testOpenClass = openclass implements testInterface with {
        field("testOpenClassField", string)
    }

    val derivedOpenTestClass = openclass extends testOpenClass implements derivedInterface with {
        field("derivedTestOpenClassField", string)
    }

    val multipleInterfaceDerivedOpenTestClass = openclass extends  testOpenClass implements listOf(marker1, marker2) with {
        field("multipleInterfaceDerivedTestOpenClassField", string)
    }

    // base structs

    val testStruct = basestruct implements testInterface with {
        field("testStructField", string)
    }

    val derivedTestStruct = basestruct extends testStruct implements derivedInterface with {
        field("derivedTestStructField", string)
    }

    val multipleInterfaceDerivedTestStruct = basestruct extends testStruct implements listOf(marker1, marker2) with {
        field("multipleInterfaceDerivedTestClassField", string)
    }

    // open structs

    val testOpenStruct = openstruct implements testInterface with {
        field("testOpenStructField", string)
    }

    val derivedOpenTestStruct = openstruct extends testOpenStruct implements derivedInterface with {
        field("derivedTestOpenStructField", string)
    }

    val multipleInterfaceDerivedOpenTestStruct = openstruct extends  testOpenStruct implements listOf(marker1, marker2) with {
        field("multipleInterfaceDerivedTestOpenStructField", string)
    }
}
