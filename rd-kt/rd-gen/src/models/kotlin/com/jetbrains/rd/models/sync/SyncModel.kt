package com.jetbrains.rd.models.sync

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.PredefinedType.*
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rd.generator.nova.util.syspropertyOrInvalid
import com.jetbrains.rd.models.sync.SyncModelRoot.Clazz
import java.io.File

object SyncModelRoot : Root(
    Kotlin11Generator(FlowTransform.Symmetric, "test.synchronization", File(syspropertyOrInvalid("model.out.src.kt.dir")))
) {
    val ClientId = contextKey("ClientId", string)
    val Baseclazz = baseclass {
        field("f", int)
    }
    val Clazz = classdef extends Baseclazz {
        property("p", int.nullable)
        map("mapPerClientId", int, int).perContext(ClientId)
    }

    init {
        field(
            "aggregate", aggregatedef("OuterAggregate") {
            property("nonNullableScalarProperty", string)
            property("nullableScalarProperty", string.nullable)
        }
        )


        property("property", Clazz)
        list("list", Clazz)
        set("set", int)
        map("map", int, Clazz)
        property("propPerClientId", int).perContext(ClientId)
    }
}

object ExtToClazz : Ext(SyncModelRoot.Clazz) {
    init {
        property("property", Clazz)
        list("list", SyncModelRoot.Clazz)
        set("set", int)
        map("map", int, SyncModelRoot.Clazz)
    }
}

//object OtherRoot : Root(
//    Kotlin11Generator(FlowTransform.Symmetric, "test.synchronization.otherRoot", File(syspropertyOrInvalid("model.out.src.kt.dir")))
//) {
////    init {
////        SyncModelRoot.initialize()
////    }
////
////    val Clazz1 = classdef extends SyncModelRoot.Baseclazz {
////        property("p", int.nullable)
////    }
//
//    init {
//        Clazz.root.setting(Kotlin11Generator.Namespace, "test.synchronization")
//        list("clazz", Clazz)
////        list("clazz1", Clazz1)
//    }
//}
