package com.jetbrains.rd.gradle.tasks

import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * Provides command line arguments for running Kotlin crosstest part
 */
open class CrossTestTaskKt : KotlinExec(), MarkedExecTask {
    override val commandLineWithArgs: List<String>
        get() = (super.getCommandLine() + tmpFile.absolutePath)

    init {
        mainClass.set("com.jetbrains.rd.cross.cases.${name}Kt")
    }
}