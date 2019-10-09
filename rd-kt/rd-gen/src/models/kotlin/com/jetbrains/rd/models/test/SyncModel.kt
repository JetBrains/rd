package com.jetbrains.rd.models.test

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.PredefinedType.*
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rd.generator.nova.util.syspropertyOrInvalid
import java.io.File

object SyncModelRoot: Root(
    Kotlin11Generator(FlowTransform.Symmetric, "test.synchronization", File(syspropertyOrInvalid("model.out.src.kt.dir")))
) {
    init {
        field("aggregate", aggregatedef("OuterAggregate") {
            property("notnullableScalar", string)
            property("nullableScalar", string)
        })
    }
}