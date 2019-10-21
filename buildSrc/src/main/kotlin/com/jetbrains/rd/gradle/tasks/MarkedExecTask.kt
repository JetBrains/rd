package com.jetbrains.rd.gradle.tasks

import java.io.File

/**
 * Provides system specific command line. It never starts directly or not.
 */
interface MarkedExecTask/* : Task*/ {
    val commandLineWithArgs: List<String>

    fun getWorkingDir(): File
}

