package com.jetbrains.rd.gradle.tasks

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input

open class RunExecTask : Exec() {
    @Input
    lateinit var execPath: String

    init {
        group = "exec"
        outputs.upToDateWhen { false }
    }

    @Suppress("UNCHECKED_CAST")
    override fun getCommandLine(): List<String> {
        val iterable = args as Iterable<String>
        return when {
            Os.isFamily(Os.FAMILY_WINDOWS) -> listOfNotNull("cmd", "/c", "$execPath.exe") + iterable
            Os.isFamily(Os.FAMILY_UNIX) -> listOfNotNull("./$execPath") + iterable
            Os.isFamily(Os.FAMILY_MAC) -> listOfNotNull("./$execPath") + iterable
            else -> error("")
        }
    }
}
