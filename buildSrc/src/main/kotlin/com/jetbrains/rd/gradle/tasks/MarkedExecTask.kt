package com.jetbrains.rd.gradle.tasks

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import java.io.File

/**
 * Provides system specific command line. It never starts directly or not.
 */
interface MarkedExecTask/* : Task*/ {
    @get:Input
    val commandLineWithArgs: List<String>

    @Internal
    fun getWorkingDir(): File

    @Internal
    fun getName(): String
}

