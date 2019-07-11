package com.jetbrains.rd.gradle.tasks

import org.gradle.api.tasks.testing.Test

open class CrossTest : Test() {
    fun addDependencies() {
        useJUnit()
        setTestNameIncludePatterns(listOf("*$name*"))
        systemProperties["CrossTestName"] = name
    }
}