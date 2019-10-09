package com.jetbrains.rd.models.test

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.PredefinedType.*
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rd.generator.nova.util.syspropertyOrInvalid
import java.io.File

object SyncModelRoot : Root(
    Kotlin11Generator(FlowTransform.Symmetric, "test.synchronization", File(syspropertyOrInvalid("model.out.src.kt.dir")))
) {
    val Baseclazz = baseclass {
        field("f", int)
    }
    val Clazz = classdef extends Baseclazz {
        property("p", int.nullable)
    }

    init {
        field(
            "aggregate", aggregatedef("OuterAggregate") {
            property("nonNullableScalarProperty", string)
            property("nullableScalarProperty", string.nullable)
        }
        )


        list("list", Clazz)
        set("set", int)
        map("map", int, Clazz)
    }
}


object ExtToClazz : Ext(SyncModelRoot.Clazz) {
    init {
        list("list", SyncModelRoot.Clazz)
        set("set", int)
        map("map", int, SyncModelRoot.Clazz)
    }
}

