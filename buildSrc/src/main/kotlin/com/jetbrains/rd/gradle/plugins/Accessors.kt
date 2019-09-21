package com.jetbrains.rd.gradle.plugins

import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler

fun DependencyHandler.`compile`(dependencyNotation: Any): Dependency? =
        add("compile", dependencyNotation)

fun DependencyHandler.`testCompile`(dependencyNotation: Any): Dependency? =
        add("testCompile", dependencyNotation)