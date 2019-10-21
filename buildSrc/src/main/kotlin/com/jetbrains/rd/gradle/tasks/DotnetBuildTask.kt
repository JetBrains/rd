package com.jetbrains.rd.gradle.tasks

import org.gradle.api.tasks.Exec
import org.gradle.process.BaseExecSpec

@Suppress("UsePropertyAccessSyntax", "LeakingThis")
open class DotnetBuildTask : Exec(), MarkedExecTask {
    override val commandLineWithArgs: String
        get() = ((this as BaseExecSpec).getCommandLine() + tmpFile.absolutePath).joinToString(separator = " ")

    companion object {
        private const val netCoreAppVersion = 2.0
    }

    init {
        executable = "dotnet"
        workingDir = workingDir.resolve("CrossTest")

        setArgs(listOf("build", "--framework=netcoreapp$netCoreAppVersion"))
    }
}