package com.jetbrains.rd.gradle.tasks

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

@Suppress("LeakingThis")
open class RunScriptTask : Exec() {
    fun execPath(value : String) {
       args(value)
    }

    init {
        group = "cmd"

        when {
            Os.isFamily(Os.FAMILY_WINDOWS) -> {
                executable = "cmd"
                args("/c")
            }
            Os.isFamily(Os.FAMILY_UNIX) -> {
                executable = "bash"
                args("-c")
            }
            Os.isFamily(Os.FAMILY_MAC) -> {
                executable = "bash"
                args("-c")
            }
        }
    }
}
