package com.jetbrains.rd.gradle.tasks

import org.gradle.api.tasks.testing.Test

open class CrossTest : Test() {
    init {
        systemProperties["CrossTestName"] = name
    }

    fun addDependencies() {
        useJUnit()
        setTestNameIncludePatterns(listOf("*$name*"))
    }
}