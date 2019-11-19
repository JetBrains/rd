package com.jetbrains.rd.gradle.plugins

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.named

fun DependencyHandler.`compile`(dependencyNotation: Any): Dependency? =
        add("compile", dependencyNotation)

fun DependencyHandler.`testCompile`(dependencyNotation: Any): Dependency? =
        add("testCompile", dependencyNotation)

fun DependencyHandler.`implementation`(dependencyNotation: Any): Dependency? =
        add("implementation", dependencyNotation)

val org.gradle.api.Project.`sourceSets`: SourceSetContainer get() =
    (this as ExtensionAware).extensions.getByName("sourceSets") as SourceSetContainer

val SourceSetContainer.`main`: NamedDomainObjectProvider<SourceSet>
    get() = named<SourceSet>("main")

val SourceSetContainer.`test`: NamedDomainObjectProvider<SourceSet>
    get() = named<SourceSet>("test")

