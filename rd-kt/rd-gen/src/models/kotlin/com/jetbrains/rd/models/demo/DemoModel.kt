@file:Suppress("unused")

package com.jetbrains.rd.models.demo

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.cpp.Cpp17Generator
import com.jetbrains.rd.generator.nova.csharp.CSharp50Generator
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rd.generator.paths.cppDirectorySystemPropertyKey
import com.jetbrains.rd.generator.paths.csDirectorySystemPropertyKey
import com.jetbrains.rd.generator.paths.ktDirectorySystemPropertyKey
import com.jetbrains.rd.generator.paths.outputDirectory

const val folder = "demo"

object DemoRoot : Root(
    Kotlin11Generator(FlowTransform.AsIs, "demo", outputDirectory(ktDirectorySystemPropertyKey, folder)),
    Cpp17Generator(FlowTransform.Reversed, "demo", outputDirectory(cppDirectorySystemPropertyKey, folder), true),
    CSharp50Generator(FlowTransform.Reversed, "demo", outputDirectory(csDirectorySystemPropertyKey, folder))
) {

    init {
        setting(Cpp17Generator.TargetName, "demo_model")
    }
}

@ExperimentalUnsignedTypes
object DemoModel : Ext(DemoRoot) {
    private val `class` = structdef("class") {
        field("true", PredefinedType.string)
    }

    private val MyEnum = enum {
        (+"default").doc("Dummy field with keyword-like name")
        +"kt"
        +"net"
        +"cpp"

        const("All", PredefinedType.int, 0)
    }

    private val MyInitializedEnum = enum {
        (+"zero")
        (+"hundred").setting(Cpp17Generator.EnumConstantValue, 100)
        (+"two")
        (+"three")
        (+"ten").setting(Cpp17Generator.EnumConstantValue, 10)
        (+"five")
        (+"six")
    }

    private val Flags = flags {
        +"anyFlag"
        +"ktFlag"
        +"netFlag"
        +"cppFlag"
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
        field("flags", Flags)
        field("myInitializedEnum", MyInitializedEnum)
    }

    private var ConstUtil = structdef {
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
        const("const_enum", MyEnum, 0)
    }

    private var Base = basestruct {
        const("const_base", PredefinedType.char, 'B')
    }

    private var OpenClass = openclass {
        property("string", PredefinedType.string)
        field("field", PredefinedType.string)
    }

    private var Derived = structdef extends Base {
        field("string", PredefinedType.string)
    }

    private var Open = openstruct extends Base {
        field("openString", PredefinedType.string)
    }

    private var OpenDerived = openstruct extends Open {
        field("openDerivedString", PredefinedType.string)
    }

    private var complicatedPair = structdef {
        field("first", Derived)
        field("second", Derived)
    }.apply {
        setting(GeneratorBase.AllowDeconstruct)
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

        property("polymorphic_open", OpenDerived)

        property("enum", MyEnum)

        property("date", PredefinedType.dateTime)

        const("const_toplevel", PredefinedType.bool, true)

        val cc = const("const_for_default", PredefinedType.string, "192")

        property("property_with_default", cc)

        property("if", `class`)

        property("my_scalars", immutableList(MyScalar))
        property("list_of_derived", immutableList(Derived))
        property("list_of_base", immutableList(Base))
    }
}

@ExperimentalUnsignedTypes
object ExtModel : Ext(DemoModel) {
    init {
        signal("checker", PredefinedType.void)
    }
}
