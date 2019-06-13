package com.jetbrains.rd.gradle.tasks;

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.tasks.Exec;
import org.gradle.api.tasks.TaskAction

open class RunExecTask : Exec() {
    lateinit var execPath: String

    init {
        group = "exec"
        outputs.upToDateWhen { false }
    }

    @TaskAction
    override fun exec() {
        println("EXEC")
        println(execPath)
        println(args)

        when {
            Os.isFamily(Os.FAMILY_WINDOWS) -> commandLine = listOf("cmd", "/c", "$execPath.exe") + args!!.toList()
            Os.isFamily(Os.FAMILY_UNIX) -> commandLine = listOf("./$execPath")
            Os.isFamily(Os.FAMILY_MAC) -> {
                //todo
            }
        }
        super.exec()
    }
}
