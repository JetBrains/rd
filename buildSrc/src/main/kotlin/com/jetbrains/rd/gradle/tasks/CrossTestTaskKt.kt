package com.jetbrains.rd.gradle.tasks

/**
 * Provides command line arguments for running Kotlin crosstest part
 */
open class CrossTestTaskKt : KotlinExec(), MarkedExecTask {
    override val commandLineWithArgs: List<String>
        get() {
            val action = execActionFactory.newJavaExecAction()
            copyTo(action)
            action.main = main
            action.classpath = classpath

            return action.commandLine + tmpFile.absolutePath
        }

    init {
        main = "com.jetbrains.rd.cross.cases.${name}Kt"
    }
}