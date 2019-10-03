package com.jetbrains.rd.gradle.tasks

import org.gradle.api.tasks.TaskAction
import java.io.File

open class CrossTestCppTask : RunExecTask(), MarkedExecTask {
    override val commandLineWithArgs: String
        get() = (super.getCommandLine() + tmpFile.absolutePath).joinToString(separator = " ")

    init {
        workingDir = File(workingDir, "build/src/rd_gen_cpp/test/cross_test/Release/")
        execPath = name
    }
}