package com.jetbrains.rd.gradle.tasks

import java.io.File

open class DotnetExecTask : RunExecTask() {
    init {
        group = "dotnet exec"
        workingDir = File(workingDir, "Cross/$name/build/")
        execPath = name
        args = args?.plus(tmpFile)
    }
}