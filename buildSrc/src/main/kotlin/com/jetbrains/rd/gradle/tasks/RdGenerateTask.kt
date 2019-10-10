package com.jetbrains.rd.gradle.tasks

import org.gradle.api.tasks.JavaExec
import java.io.File

/**
 * Uses compiled RdGen for generating prepared models
 */
open class RdGenerateTask : JavaExec() {
    val sourceDirectories = mutableListOf<File>()

    init {
        group = "generate"
        main = "com.jetbrains.rd.generator.nova.MainKt"
    }

    override fun exec() {
        println("Starting GenerateTask args=${args}")

        super.exec()

        println("Finishing GenerateTask args=${args}")
    }

    fun addSourceDirectory(file: File) {
        sourceDirectories.add(file)
        inputs.dir(file)
    }

    fun addOutputDirectories(properties: Map<String, String>) {
        systemProperties(properties)
        outputs.dirs(properties.values.toList())
    }
}