package com.jetbrains.rd.generator.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class RdGenPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create("rdgen", RdGenExtension::class.java, project)
        project.tasks.create("rdgen", RdGenTask::class.java)
    }
}