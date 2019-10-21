package com.jetbrains.rd.gradle.tasks

import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

@Suppress("LeakingThis", "LeakingThis")
open class CrossTestKtRdTask : KotlinExec(), MarkedExecTask {
    override val commandLineWithArgs: List<String>
        get() = (super.getCommandLine() + tmpFile.absolutePath)

    init {
        main = "com.jetbrains.rd.cross.${name}Kt"
    }
}