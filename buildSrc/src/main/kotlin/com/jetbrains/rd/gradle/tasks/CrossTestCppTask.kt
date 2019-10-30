package com.jetbrains.rd.gradle.tasks

import java.io.File

/**
 * Provides command line arguments for running C++ crosstest part
 */
open class CrossTestCppTask : RunExecTask(), MarkedExecTask {
    @Suppress("UNCHECKED_CAST")
    override val commandLineWithArgs: List<String>
        get() = super.getCommandLine() + tmpFile.absolutePath

    init {
        workingDir = File(workingDir, "build/src/rd_gen_cpp/test/cross_test/Release/")
        execPath = name
    }
}