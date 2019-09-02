package com.jetbrains.rd.gradle.tasks

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

open class RunScriptTask : Exec() {
    @Input
    lateinit var execPath: String

    init {
        group = "cmd"
        outputs.upToDateWhen { true }
    }

    @TaskAction
    override fun exec() {
        val iterable = args as Iterable<*>
        when {
            Os.isFamily(Os.FAMILY_WINDOWS) -> commandLine = listOfNotNull("cmd", "/c", execPath) + iterable
            Os.isFamily(Os.FAMILY_UNIX) -> commandLine = listOfNotNull("bash", execPath, "-c") + iterable
            Os.isFamily(Os.FAMILY_MAC) -> commandLine = listOfNotNull("bash", execPath, "-c") + iterable
        }
        super.exec()
    }
}
