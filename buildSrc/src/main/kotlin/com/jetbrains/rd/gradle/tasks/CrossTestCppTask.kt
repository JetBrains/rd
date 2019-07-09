package com.jetbrains.rd.gradle.tasks

import java.io.File

open class CrossTestCppTask : RunExecTask() {
    init {
        workingDir = File(workingDir, "build/src/rd_gen_cpp/test/cross_test/Release/")
        execPath = name
        args = args?.plus(tmpFilePath)
    }
}