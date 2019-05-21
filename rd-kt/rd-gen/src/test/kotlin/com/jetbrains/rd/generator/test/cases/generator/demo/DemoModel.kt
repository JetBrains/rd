package com.jetbrains.rd.generator.test.cases.generator.demo

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.cpp.Cpp17Generator
import com.jetbrains.rd.generator.nova.csharp.CSharp50Generator
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rd.generator.nova.util.syspropertyOrInvalid
import java.io.File

object DemoRoot : Root(
        Kotlin11Generator(FlowTransform.Reversed, "demo", File(syspropertyOrInvalid("model.out.src.kt.dir"))),
        Cpp17Generator(FlowTransform.AsIs, "demo", File(syspropertyOrInvalid("model.out.src.cpp.dir"))),
        CSharp50Generator(FlowTransform.AsIs, "demo", File(syspropertyOrInvalid("model.out.src.cs.dir")))
) {
    init {
        setting(Cpp17Generator.TargetName, "demo_model")
    }
}

object DemoModel : Ext(DemoRoot) {
    private var MyScalar = structdef {
        field("bool", PredefinedType.bool)
        field("byte", PredefinedType.byte)
        field("short", PredefinedType.short)
        field("int", PredefinedType.int)
        field("long", PredefinedType.long)
        field("float", PredefinedType.float)
        field("double", PredefinedType.double)
        field("unsigned_short", PredefinedType.short.unsigned())
        field("unsigned_int", PredefinedType.int.unsigned())
        field("unsigned_long", PredefinedType.long.unsigned())
    }

    private var Base = basestruct {

    }

    private var Derived = structdef extends Base {
        field("string", PredefinedType.string)
    }

    init {
        property("boolean_property", PredefinedType.bool)

        property("scalar", MyScalar)

        list("list", PredefinedType.int)

        set("set", PredefinedType.int)

        map("mapLongToString", PredefinedType.long, PredefinedType.string)

        call("call", PredefinedType.char, PredefinedType.string)

        callback("callback", PredefinedType.string, PredefinedType.int)

        property("interned_string", PredefinedType.string.interned(ProtocolInternScope))

        property("polymorphic", Base)
    }
}

object ExtModel : Ext(DemoModel) {
    init {
        signal("checker", PredefinedType.void)
    }
}
