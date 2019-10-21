package com.jetbrains.rd.gradle.tasks

import com.jetbrains.rd.gradle.dependencies.netCoreAppVersion
import org.gradle.api.tasks.Exec
import org.gradle.process.BaseExecSpec

@Suppress("UsePropertyAccessSyntax", "LeakingThis")
open class DotnetBuildTask : Exec(), MarkedExecTask {
    override val commandLineWithArgs: List<String>
        get() = ((this as BaseExecSpec).getCommandLine() + tmpFile.absolutePath)

    init {
        executable = "dotnet"
        workingDir = workingDir.resolve("CrossTest")

        setArgs(listOf("build", "--framework=netcoreapp$netCoreAppVersion"))
    }
}