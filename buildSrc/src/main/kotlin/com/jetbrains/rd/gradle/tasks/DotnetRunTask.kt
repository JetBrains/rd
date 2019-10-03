package com.jetbrains.rd.gradle.tasks

import org.gradle.api.tasks.TaskAction
import org.gradle.process.BaseExecSpec

@Suppress("UsePropertyAccessSyntax", "LeakingThis")
open class DotnetRunTask : RunExecTask(), MarkedExecTask {
    override val commandLineWithArgs: String
        get() = ((this as BaseExecSpec).getCommandLine() + tmpFile.absolutePath).joinToString(separator = " ")

    companion object {
        private const val netCoreAppVersion = 2.0
    }

    init {
        executable = "dotnet"
        workingDir = workingDir.resolve("CrossTest")

        setArgs(listOf("run", "--framework=netcoreapp$netCoreAppVersion", name))
    }
}