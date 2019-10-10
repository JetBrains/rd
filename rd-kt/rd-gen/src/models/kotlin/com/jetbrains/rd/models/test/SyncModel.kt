package com.jetbrains.rd.models.test

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.PredefinedType.*
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rd.generator.nova.util.syspropertyOrInvalid
import com.jetbrains.rd.models.test.SyncModelRoot.Clazz
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


object OtherRoot : Root(
    Kotlin11Generator(FlowTransform.Symmetric, "test.synchronization.otherRoot", File(syspropertyOrInvalid("model.out.src.kt.dir")))
) {
    init {
        SyncModelRoot.initialize()
    }

    val Clazz1 = classdef extends SyncModelRoot.Baseclazz {
        property("p", int.nullable)
    }

    init {
        Clazz.root.setting(Kotlin11Generator.Namespace, "test.synchronization")
        list("clazz", Clazz)
        list("clazz1", Clazz1)
    }
}
