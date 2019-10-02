package com.jetbrains.rd.gradle.tasks

import org.gradle.api.tasks.TaskAction
import java.io.File

open class DotnetRunTask : RunExecTask() {
    init {
        executable = "dotnet"
        workingDir = File(workingDir, "Cross")
        setArgs(listOf("run", "-c:Configuration=Release", name))
    }

    @TaskAction
    override fun exec() {
        setArgs(args?.plus(tmpFile.absolutePath))
        super.exec()
    }
}