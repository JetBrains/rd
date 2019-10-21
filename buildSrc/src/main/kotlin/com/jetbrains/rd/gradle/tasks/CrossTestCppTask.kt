package com.jetbrains.rd.gradle.tasks

import java.io.File

open class CrossTestCppTask : RunExecTask(), MarkedExecTask {
    @Suppress("UNCHECKED_CAST")
    override val commandLineWithArgs: List<String>
        get() = super.getCommandLine() as List<String> + tmpFile.absolutePath

    init {
        workingDir = File(workingDir, "build/src/rd_gen_cpp/test/cross_test/Release/")
        execPath = name
    }
}