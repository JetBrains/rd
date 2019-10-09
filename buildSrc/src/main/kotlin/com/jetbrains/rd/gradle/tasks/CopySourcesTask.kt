package com.jetbrains.rd.gradle.tasks

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.creating
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.io.File
import javax.inject.Inject

/**
 * Copy sources from {generativeSourceSet.output} to {currentProject.buildDir.resolve("generated")}
 */
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

fun Project.creatingCopySourcesTask(currentSourceSet: NamedDomainObjectProvider<KotlinSourceSet>, generativeSourceSet: SourceSet) =
    tasks.creating(CopySourcesTask::class) {
        this@creating.currentSourceSet = currentSourceSet.get()
        currentProject = this@creatingCopySourcesTask
        this@creating.generativeSourceSet = generativeSourceSet

        lateInit()
    }