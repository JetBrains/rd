package com.jetbrains.rd.gradle.tasks

import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.SourceSet
import javax.inject.Inject
import org.jetbrains.kotlin.gradle.plugin.*
import java.io.File

open class CopySourcesTask @Inject constructor() : Exec() {
    @Input
    lateinit var currentSourceSet: KotlinSourceSet
    @Input
    lateinit var currentProject: Project
    @Input
    lateinit var generativeSourceSet: SourceSet

    private lateinit var generatedDir: File

    init {
        group = "copy generated sources"
    }

    fun lateInit() {
        generatedDir = currentProject.buildDir.resolve("generated")
        currentSourceSet.kotlin.srcDirs(generatedDir.absolutePath)
    }

    public override fun exec() {
        println("CopySourcesTask")
        copyGeneratedSources()
    }

    private fun copyGeneratedSources() {
        generatedDir.mkdirs()
        generativeSourceSet.output.dirs.forEach { dir ->
            dir.copyRecursively(File(generatedDir, dir.name), overwrite = true)
        }
    }
}
