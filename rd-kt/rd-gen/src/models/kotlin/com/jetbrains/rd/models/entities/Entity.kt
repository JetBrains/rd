package com.jetbrains.rd.models.entities

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.cpp.Cpp17Generator
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rd.generator.paths.cppDirectorySystemPropertyKey
import com.jetbrains.rd.generator.paths.ktDirectorySystemPropertyKey
import com.jetbrains.rd.generator.paths.outputDirectory

const val folder = "entities"

object EntityRoot : Root(
        Kotlin11Generator(FlowTransform.Reversed, "org.example", outputDirectory(ktDirectorySystemPropertyKey, folder)),
        Cpp17Generator(FlowTransform.AsIs, "rd::test::util", outputDirectory(cppDirectorySystemPropertyKey, folder), usingPrecompiledHeaders=true)
) {
    init {
        setting(Cpp17Generator.TargetName, "entities")
    }
}

@Suppress("unused")
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

    init {
        property("bar", PredefinedType.string)
    }
}