package com.jetbrains.rd.generator.test.cases.generator.demo

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.cpp.Cpp17Generator
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rd.generator.nova.util.syspropertyOrInvalid
import java.io.File

object DemoRoot : Root(
        Kotlin11Generator(FlowTransform.Reversed, "org.example", File(syspropertyOrInvalid("model.out.src.kt.dir"))),
        Cpp17Generator(FlowTransform.AsIs, "org.example", File(syspropertyOrInvalid("model.out.src.cpp.dir")))
//    CSharp50Generator(FlowTransform.Reversed, "org.example", File("C:/work/Rider/Platform/RdProtocol/rider-generated/Src//com/jetbrains/rider/model.cSharp"), "[ShellComponent]")
)

object DemoModel : Ext(DemoRoot) {
    private var MyScalar = structdef {
        field("sign", PredefinedType.bool)
        field("byte", PredefinedType.byte)
        field("short", PredefinedType.short)
        field("int", PredefinedType.int)
        field("long", PredefinedType.long)
//        field("float_", PredefinedType.float)
//        field("double_", PredefinedType.double)
    }

    init {
        property("boolean_property", PredefinedType.bool)

        property("scalar", MyScalar)

        list("list", PredefinedType.int)

        set("set", PredefinedType.int)

        map("mapLongToString", PredefinedType.long, PredefinedType.string)

        call("call", PredefinedType.char, PredefinedType.string)

        callback("callback", PredefinedType.string, PredefinedType.int);
    }
}

object ExtModel : Ext(DemoModel) {
    init {
        signal("checker", PredefinedType.void)
    }
}
