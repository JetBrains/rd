package com.jetbrains.rd.gradle.tasks

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.JavaExec
import java.io.File

/**
 * Uses compiled RdGen for generating prepared models
 */
open class RdGenerateTask : JavaExec() {
    @Input
    lateinit var sourcesRoot: File
    @Input
    lateinit var sourcesFolder: String

    init {
        group = "generate"
        main = "com.jetbrains.rd.generator.nova.MainKt"
    }

    override fun exec() {
        println("Starting GenerateTask sourcesRoot=$sourcesRoot, sourcesFolder=$sourcesFolder")

        super.exec()

        println("Finishing GenerateTask sourcesRoot=$sourcesRoot, sourcesFolder=$sourcesFolder")
    }

    fun addSourcesDirectories(properties: Map<String, String>) {
        systemProperties(properties)

        outputs.dirs(properties.values.toList())
    }
}