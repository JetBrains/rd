package com.jetbrains.rider.generator.test.cases.generator.demo

import com.jetbrains.rider.generator.nova.*
import com.jetbrains.rider.generator.nova.cpp.Cpp17Generator
import com.jetbrains.rider.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rider.generator.nova.util.syspropertyOrEmpty
import java.io.File

object DemoRoot : Root(
        Kotlin11Generator(FlowTransform.Reversed, "org.example", File(syspropertyOrEmpty("model.out.src.kt.dir"))),
        Cpp17Generator(FlowTransform.Reversed, "org.example", File(syspropertyOrEmpty("model.out.src.cpp.dir")))
//    CSharp50Generator(FlowTransform.Reversed, "org.example", File("C:/work/Rider/Platform/RdProtocol/rider-generated/Src//com/jetbrains/rider/model.cSharp"), "[ShellComponent]")
)

object DemoModel : Ext(DemoRoot) {
    private var MyList = classdef {
        list("list", PredefinedType.string)
    }

    private var MyState = classdef {
        property("enumProperty", enum("") {
            +"NONE"
            +"ONE"
            +"TWO"
            +"THREE"
        })
        property("string_property", "1")
    }

    private var MyScalar = structdef {
        field("sign_", PredefinedType.bool)
        field("byte_", PredefinedType.byte)
        field("short_", PredefinedType.short)
        field("int_", PredefinedType.int)
        field("long_", PredefinedType.long)
//        field("float_", PredefinedType.float)
//        field("double_", PredefinedType.double)
    }

//    private var MyExt = Ext(, extName = "ext")

    init {
        signal("voidSignal", PredefinedType.void)

        property("propertyList", MyList)

        map("mapLongToString", PredefinedType.long, PredefinedType.string)

        property("state", MyState)

        call("callCharToString", PredefinedType.char, PredefinedType.string)

        property("propertyOfArray", array(PredefinedType.char))

        property("scalar", MyScalar)

        set("set", PredefinedType.int)


    }
}

object ExtModel: Ext(DemoModel) {
    private val ColorChooserParam = structdef {
        field("caption", PredefinedType.string.nullable)
    }

    private val ColorChooserSession = classdef("ColorChooserSession") {
        field("param", ColorChooserParam)
        signal("colorChanged", PredefinedType.void)
    }

    init {
//        list("ColorList", ColorChooserSession)
        signal("checkExtension", PredefinedType.void)
    }
}
