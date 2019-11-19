package com.jetbrains.rd.gradle.tasks

import org.gradle.process.BaseExecSpec

/**
 * Provides command line arguments for running C# crosstest part
 */
open class CrossTestCsTask : DotnetRunTask(), MarkedExecTask {
    override val commandLineWithArgs: List<String>
        get() = ((this as BaseExecSpec).commandLine + tmpFile.absolutePath)
}