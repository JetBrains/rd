package com.jetbrains.rd.gradle.plugins

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.getting
import org.jetbrains.dokka.gradle.DokkaTask
import org.gradle.kotlin.dsl.*

fun Project.createPackageJavaDoc(files: ConfigurableFileCollection): Jar {
    val dokka by tasks.getting(DokkaTask::class) {
        outputFormat = "html"
        outputDirectory = "$buildDir/javadoc"
        kotlinTasks {
            emptyList()
        }
        reportUndocumented = false
        sourceDirs = files
    }

    val packageJavadoc by tasks.creating(Jar::class) {
        dependsOn(dokka)
        from("$buildDir/javadoc")
        archiveClassifier.set("javadoc")
    }

    return packageJavadoc
}