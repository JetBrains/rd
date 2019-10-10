package com.jetbrains.rd.models.test

import com.jetbrains.rd.generator.nova.FlowTransform
import com.jetbrains.rd.generator.nova.PredefinedType.string
import com.jetbrains.rd.generator.nova.Root
import com.jetbrains.rd.generator.nova.field
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rd.generator.nova.property
import com.jetbrains.rd.util.paths.ktDirectorySystemPropertyKey
import com.jetbrains.rd.util.paths.*

const val folder = "test"

@Suppress("unused")
object SyncModelRoot: Root(
    Kotlin11Generator(FlowTransform.Symmetric, "test.synchronization", outputDirectory(ktDirectorySystemPropertyKey, folder))
) {
    init {
        field("aggregate", aggregatedef("OuterAggregate") {
            property("notnullableScalar", string)
            property("nullableScalar", string)
        })
    }
}