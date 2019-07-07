package com.jetbrains.rd.gradle.tasks

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.TaskAction

open class RunCmdTask : Exec() {
    lateinit var execPath: String

    init {
        group = "cmd"
//        outputs.upToDateWhen { false }
    }

    @TaskAction
    override fun exec() {
        when {
            Os.isFamily(Os.FAMILY_WINDOWS) -> commandLine = listOf("cmd", "/c", execPath)
            Os.isFamily(Os.FAMILY_UNIX) -> commandLine = listOf("./$execPath")
            Os.isFamily(Os.FAMILY_MAC) -> commandLine = listOf("./$execPath")
        }
        super.exec()
    }
}
