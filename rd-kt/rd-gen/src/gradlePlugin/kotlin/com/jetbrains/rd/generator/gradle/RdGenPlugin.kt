package com.jetbrains.rd.generator.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class RdGenPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create("rdgen", RdGenExtension::class.java, project)
        project.configurations.create("rdGenConfiguration")
        project.tasks.create("rdgen", RdGenTask::class.java)

        project.dependencies.run {
            add("rdGenConfiguration", "org.jetbrains.kotlin:kotlin-compiler-embeddable:1.8.10")
            add("rdGenConfiguration", "org.jetbrains.kotlin:kotlin-stdlib:1.8.10")
            add("rdGenConfiguration", "org.jetbrains.kotlin:kotlin-reflect:1.8.10")
            add("rdGenConfiguration", "org.jetbrains.kotlin:kotlin-stdlib-common:1.8.10")
        }
    }
}