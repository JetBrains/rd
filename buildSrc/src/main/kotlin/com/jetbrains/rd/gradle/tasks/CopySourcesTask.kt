package com.jetbrains.rd.gradle.tasks

import com.jetbrains.rd.gradle.tasks.util.copyGeneratedSources
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.SourceSet
import javax.inject.Inject

open class CopySourcesTask @Inject constructor() : Exec() {
    @Input
    lateinit var currentSourceSet: SourceSet
    @Input
    lateinit var currentProject: Project
    @Input
    lateinit var generativeSourceSet: SourceSet

    init {
        group = "copy generated sources"
    }

    public override fun exec() {
        println("CopySourcesTask")
        copyGeneratedSources(currentSourceSet, currentProject, generativeSourceSet)

    }
}
