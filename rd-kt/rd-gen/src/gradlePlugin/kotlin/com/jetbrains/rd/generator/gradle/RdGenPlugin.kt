package com.jetbrains.rd.generator.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class RdGenPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create("rdgen", RdGenExtension::class.java, project)
        project.configurations.create("rdGenConfiguration")
        project.tasks.create("rdgen", RdGenTask::class.java)

        project.dependencies.run {
            add("rdGenConfiguration", "org.jetbrains.kotlin:kotlin-compiler-embeddable:1.7.0")
            add("rdGenConfiguration", "org.jetbrains.kotlin:kotlin-stdlib:1.7.0")
            add("rdGenConfiguration", "org.jetbrains.kotlin:kotlin-reflect:1.7.0")
            add("rdGenConfiguration", "org.jetbrains.kotlin:kotlin-stdlib-common:1.7.0")
            add("rdGenConfiguration", "org.jetbrains.intellij.deps:trove4j:1.0.20181211")
        }
    }
}