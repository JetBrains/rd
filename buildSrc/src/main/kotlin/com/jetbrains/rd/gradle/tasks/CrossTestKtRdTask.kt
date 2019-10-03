package com.jetbrains.rd.gradle.tasks

@Suppress("LeakingThis", "LeakingThis")
open class CrossTestKtRdTask : KotlinExec(), MarkedExecTask {
    override val commandLineWithArgs: String
        get() = (super.getCommandLine() + tmpFile.absolutePath).joinToString(separator = " ")

    init {
        main = "com.jetbrains.rd.cross.${name}Kt"
    }
}