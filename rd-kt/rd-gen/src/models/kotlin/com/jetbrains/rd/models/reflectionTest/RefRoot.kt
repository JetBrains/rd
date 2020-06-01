@file:Suppress("unused")

package com.jetbrains.rd.models.reflectionTest

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.csharp.CSharp50Generator
import com.jetbrains.rd.generator.paths.csDirectorySystemPropertyKey
import com.jetbrains.rd.generator.paths.outputDirectory
import java.io.File

val outFolder = outputDirectory(csDirectorySystemPropertyKey, "reflectionTest")

object RefRoot : Root(
    CSharp50Generator(FlowTransform.AsIs, "Test.RdFramework.Reflection.Generated", outFolder)
) {
    init {
        setting(CSharp50Generator.FsPath) { File(it.folder, "${this.name}.cs") }
    }
}

object RefExt : Ext(RefRoot) {
    init {
        setting(CSharp50Generator.FsPath) { File(it.folder, "${this.name}.cs") }
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
       property("Struct", Base)
       property("OpenModel", OpenClass)
    }
}
