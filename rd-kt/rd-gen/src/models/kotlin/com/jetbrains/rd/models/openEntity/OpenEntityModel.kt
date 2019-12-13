package com.jetbrains.rd.models.openEntity

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.PredefinedType.*
import com.jetbrains.rd.generator.nova.csharp.CSharp50Generator
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rd.generator.paths.csDirectorySystemPropertyKey
import com.jetbrains.rd.generator.paths.ktDirectorySystemPropertyKey
import com.jetbrains.rd.generator.paths.outputDirectory
import com.jetbrains.rd.models.demo.folder

object OpenEntityRoot : Root(
    Kotlin11Generator(FlowTransform.AsIs, "com.jetbrains.rd.framework.test.cases.openEntity", outputDirectory(ktDirectorySystemPropertyKey, "openEntity")),
    CSharp50Generator(FlowTransform.Reversed, "entity", outputDirectory(csDirectorySystemPropertyKey, folder))
)

object OpenEntityModel : Ext(OpenEntityRoot){

    // open classes

    private val baseClassEntity = baseclass{
        property("baseClassProperty", bool)
    }

    private val baseOpenClassEntity = openclass extends baseClassEntity {
        property("baseOpenClassProperty", string)
    }

    private val openClassEntity = openclass extends baseOpenClassEntity {
        property("openClassProperty", int)
    }

    private val concreteClassEntity = classdef extends openClassEntity {
        property("concreteProperty", double)
    }



    // open structs

    private val baseStructEntity = basestruct{
        field("baseStructField", bool)
    }

    private val baseOpenStructEntity = openstruct extends baseStructEntity {
        field("baseOpenStructField", string)
    }

    private val openStructEntity = openstruct extends baseOpenStructEntity {
        field("openStructField", int)
    }

    private val concreteStructEntity = structdef extends openStructEntity {
        field("concreteField", double)
    }
}