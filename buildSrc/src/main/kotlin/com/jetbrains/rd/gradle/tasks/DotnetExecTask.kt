package com.jetbrains.rd.gradle.tasks

import org.gradle.api.tasks.TaskAction
import java.io.File

open class DotnetExecTask : RunExecTask() {
    init {
        group = "dotnet exec"
        workingDir = File(workingDir, "Cross/$name/build/")
        execPath = name
    }

    @TaskAction
    override fun exec() {
        setArgs(args?.plus(tmpFile.absolutePath))
        super.exec()
    }
}