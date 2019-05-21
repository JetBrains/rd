package com.jetbrains.rd.generator.test.cases.generator.entities

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.cpp.Cpp17Generator
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rd.generator.nova.util.syspropertyOrInvalid
import java.io.File

object EntityRoot : Root(
        Kotlin11Generator(FlowTransform.Reversed, "org.example", File(syspropertyOrInvalid("model.out.src.kt.dir"))),
        Cpp17Generator(FlowTransform.AsIs, "rd.test.util", File(syspropertyOrInvalid("model.out.src.cpp.dir")))
//    CSharp50Generator(FlowTransform.Reversed, "org.example", File("C:/work/Rider/Platform/RdProtocol/rider-generated/Src//com/jetbrains/rider/model.cSharp"), "[ShellComponent]")
) {
    init {
        setting(Cpp17Generator.TargetName, "entities")
    }
}

object DynamicExt : Ext(EntityRoot) {
    private var AbstractEntity = basestruct {
        field("name", PredefinedType.string)
    }

    private var ConcreteEntity = structdef extends AbstractEntity {
        field("stringValue", PredefinedType.string)
    }

    private var FakeEntity = structdef extends AbstractEntity {
        field("booleanValue", PredefinedType.bool)
    }

    private var DynamicEntity = classdef {
        property("foo", PredefinedType.int)
    }

    //    private var DynamicEntity = classdef {
    init {
        property("bar", PredefinedType.string)
    }
//    }

//    private var DynamicExt =
}