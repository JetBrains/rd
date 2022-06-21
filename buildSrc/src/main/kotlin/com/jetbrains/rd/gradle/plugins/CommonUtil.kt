package com.jetbrains.rd.gradle.plugins

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.bundling.Jar
import org.jetbrains.dokka.gradle.DokkaTask
import org.gradle.kotlin.dsl.*

fun Project.createPackageJavaDoc(files: ConfigurableFileCollection): Jar {
    val dokkaHtml by tasks.named<DokkaTask>("dokkaHtml") {
        outputDirectory.set(file("$buildDir/javadoc"))
        dokkaSourceSets {
            named("main") {
                sourceRoots.from(files)
                reportUndocumented.set(false)
            }

        }
    }

    val packageJavadoc by tasks.creating(Jar::class) {
        dependsOn(dokkaHtml)
        from("$buildDir/javadoc")
        archiveClassifier.set("javadoc")
    }

    return packageJavadoc
}