package com.jetbrains.rd.gradle.tasks

import com.jetbrains.rd.gradle.dependencies.netCoreAppVersion
import org.gradle.api.tasks.Exec
import org.gradle.process.BaseExecSpec

@Suppress("UsePropertyAccessSyntax", "LeakingThis")
open class DotnetBuildTask : Exec() {
    init {
        executable = "dotnet"

        setArgs(listOf("build"))

        addInputs()
        addOutputs()
    }

    private fun addInputs() {
        val blackListFolder = listOf(".idea", "bin", "obj", "packages", "artifacts")
        val whiteListExtensions = listOf("cs", "csproj", "fs")
        val search = workingDir.walk()
                .onEnter { a -> a.name !in blackListFolder }
                .filter { a -> a.isFile && a.extension in whiteListExtensions }
        search.forEach { inputs.file(it) }
    }

    private fun addOutputs() {
        val whiteListExtensions = listOf("dll", "exe")
        val search = workingDir.walk()
            .filter { a -> a.isFile && a.extension in whiteListExtensions }
        search.forEach { outputs.file(it) }
    }
}