package com.jetbrains.rd.gradle.tasks

import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input

/**
 * Build dotnet project using "dotnet" command line tool
 */
@Suppress("UsePropertyAccessSyntax", "LeakingThis")
open class DotnetBuildTask : Exec() {
    fun configuration(value: String) {
        args("-c=${value}")
    }

    init {
        executable = "dotnet"

        args("build")

        addInputs()
        addOutputs()
    }

    private fun addInputs() {
        val excludedFolder = listOf(".idea", "bin", "obj", "packages", "artifacts")
        val includedExtensions = listOf("cs", "csproj", "fs")
        val search = workingDir.walk()
            .onEnter { a -> a.name !in excludedFolder }
            .filter { a -> a.isFile && a.extension in includedExtensions }
        search.forEach { inputs.file(it) }
    }

    private fun addOutputs() {
        val includedExtensions = listOf("dll", "exe")
        val search = workingDir.walk()
            .filter { a -> a.isFile && a.extension in includedExtensions }
        search.forEach { outputs.file(it) }

    }
}