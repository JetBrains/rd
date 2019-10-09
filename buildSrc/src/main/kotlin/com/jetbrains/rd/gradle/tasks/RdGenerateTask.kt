package com.jetbrains.rd.gradle.tasks

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.JavaExec
import java.io.File

/**
 * Uses compiled RdGen for generating prepared models
 */
open class RdGenerateTask : JavaExec() {
    /**
     * Directory's parent where sources are placed
     */
    @Input
    lateinit var sourcesRoot: File
    /**
     * Name of directory where sources are placed
     */
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

    fun addOutputDirectories(properties: Map<String, String>) {
        systemProperties(properties)

        outputs.dirs(properties.values.toList())
    }
}