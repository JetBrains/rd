package com.jetbrains.rd.gradle.tasks

import org.gradle.api.tasks.TaskAction
import java.io.File

open class CrossTestCppTask : RunExecTask() {
    init {
        workingDir = File(workingDir, "build/src/rd_gen_cpp/test/cross_test/Release/")
        execPath = name
    }

    @TaskAction
    override fun exec() {
        args = args?.plus(tmpFile.absolutePath)
        super.exec()
    }
}