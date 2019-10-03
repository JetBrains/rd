package com.jetbrains.rd.gradle.tasks

import org.gradle.api.Task
import java.io.File

/**
 * Provides system specific command line. It never starts directly or not.
 */
interface MarkedExecTask/* : Task*/ {
    val commandLineWithArgs: String

    fun getWorkingDir(): File

}