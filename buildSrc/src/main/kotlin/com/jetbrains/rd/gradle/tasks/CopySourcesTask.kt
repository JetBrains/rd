package com.jetbrains.rd.gradle.tasks

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.creating
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.io.File
import javax.inject.Inject

/**
 * Copy sources from {generativeSourceSet.output} to {currentProject.buildDir.resolve("generated")}
 */
open class CopySourcesTask @Inject constructor() : Exec() {
    @Internal
    lateinit var currentSourceSet: KotlinSourceSet
    @Internal
    lateinit var currentProject: Project
    @Internal
    lateinit var generativeTask: TaskProvider<Task>

    private lateinit var generatedDir: File

    init {
        group = "copy generated sources"
    }

    fun lateInit() {
        generatedDir = currentProject.buildDir.resolve("generated")

        inputs.files(generativeTask.get().outputs)
        outputs.dirs(generatedDir)

        currentSourceSet.kotlin.srcDirs(generatedDir.absolutePath)
    }

    public override fun exec() {
        copyGeneratedSources()
    }

    private fun copyGeneratedSources() {
        generatedDir.mkdirs()
        generativeTask.get().outputs.files.forEach { file ->
            file.copyRecursively(File(generatedDir, file.name), overwrite = true)
        }
    }
}

fun Project.creatingCopySourcesTask(currentSourceSet: NamedDomainObjectProvider<KotlinSourceSet>,
                                    generativeTask: TaskProvider<Task>) =
    tasks.creating(CopySourcesTask::class) {
        dependsOn(generativeTask)

        this.currentSourceSet = currentSourceSet.get()
        this.currentProject = this@creatingCopySourcesTask
        this.generativeTask = generativeTask

        lateInit()
    }