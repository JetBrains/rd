package com.jetbrains.rd.gradle.tasks;

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Action
import org.gradle.api.Task
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
        val iterable = args as Iterable<*>
        when {
            Os.isFamily(Os.FAMILY_WINDOWS) -> commandLine = listOfNotNull("cmd", "/c", "$execPath.exe") + iterable
            Os.isFamily(Os.FAMILY_UNIX) -> commandLine = listOfNotNull("./$execPath") + iterable
            Os.isFamily(Os.FAMILY_MAC) -> commandLine = listOfNotNull("./$execPath") + iterable
        }
        super.exec()
    }
}
