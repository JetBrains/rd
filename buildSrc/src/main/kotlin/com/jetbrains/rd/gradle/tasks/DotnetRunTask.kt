package com.jetbrains.rd.gradle.tasks

import com.jetbrains.rd.gradle.dependencies.netCoreAppVersion
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.create
import org.gradle.process.BaseExecSpec

/**
 * Run dotnet project using "dotnet" command line tool and "netcoreapp" as framework.
 */
@Suppress("UsePropertyAccessSyntax", "LeakingThis")
open class DotnetRunTask : Exec(), MarkedExecTask {
    override val commandLineWithArgs: List<String>
        get() = ((this as BaseExecSpec).getCommandLine() + tmpFile.absolutePath)

    init {
        executable = "dotnet"

        setArgs(listOf("run", "--framework=netcoreapp$netCoreAppVersion", name))
    }
}