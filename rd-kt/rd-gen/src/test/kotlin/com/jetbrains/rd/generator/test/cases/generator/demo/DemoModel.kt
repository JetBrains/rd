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

@ExperimentalUnsignedTypes
object DemoModel : Ext(DemoRoot) {
    private var MyEnum = enum {
        +"default"
        +"kt"
        +"net"
        +"cpp"
    }

    private var MyScalar = structdef {
        field("bool", PredefinedType.bool)
        field("byte", PredefinedType.byte)
        field("short", PredefinedType.short)
        field("int", PredefinedType.int)
        field("long", PredefinedType.long)
        field("float", PredefinedType.float)
        field("double", PredefinedType.double)
        field("unsigned_byte", PredefinedType.ubyte)
        field("unsigned_short", PredefinedType.ushort)
        field("unsigned_int", PredefinedType.uint)
        field("unsigned_long", PredefinedType.ulong)
        field("enum", MyEnum)

    }

    private var ConstUtil  = structdef {
        const("const_byte", PredefinedType.byte, 0)
        const("const_short", PredefinedType.short, Short.MAX_VALUE)
        const("const_int", PredefinedType.int, Int.MAX_VALUE)
        const("const_long", PredefinedType.long, Long.MAX_VALUE)
        const("const_ubyte", PredefinedType.ubyte, UByte.MAX_VALUE)
        const("const_ushort", PredefinedType.ushort, UShort.MAX_VALUE)
        const("const_uint", PredefinedType.uint, UInt.MAX_VALUE)
        const("const_ulong", PredefinedType.ulong, ULong.MAX_VALUE)
        const("const_float", PredefinedType.float, 0f)
        const("const_double", PredefinedType.double, 0.0)
        const("const_string", PredefinedType.string, "const_string_value")
        const("const_enum", MyEnum, MyEnum.constants[0])
    }

    private var Base = basestruct {
        const("const_base", PredefinedType.char, 'B')
    }

    private var Derived = structdef extends Base {
        field("string", PredefinedType.string)
    }

    init {
        property("boolean_property", PredefinedType.bool)

        property("boolean_array", array(PredefinedType.bool))

        property("scalar", MyScalar)

        property("ubyte", PredefinedType.ubyte)

        property("ubyte_array", array(PredefinedType.ubyte))

        list("list", PredefinedType.int)

        set("set", PredefinedType.int)

        map("mapLongToString", PredefinedType.long, PredefinedType.string)

        call("call", PredefinedType.char, PredefinedType.string)

        callback("callback", PredefinedType.string, PredefinedType.int)

        property("interned_string", PredefinedType.string.interned(ProtocolInternScope))

        property("polymorphic", Base)

        property("enum", MyEnum)

        const("const_toplevel", PredefinedType.bool, true)
    }
}

object ExtModel : Ext(DemoModel) {
    init {
        signal("checker", PredefinedType.void)
    }
}
