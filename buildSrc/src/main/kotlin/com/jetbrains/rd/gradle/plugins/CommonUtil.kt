package com.jetbrains.rd.gradle.plugins

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import org.jetbrains.dokka.gradle.DokkaExtension

fun Project.createPackageJavaDoc(files: ConfigurableFileCollection): Jar {
    
    extensions.configure(DokkaExtension::class.java) {
        dokkaPublications.named("html") {
            outputDirectory.set(file("$buildDir/javadoc"))
        }

        dokkaSourceSets.named("main") {
            sourceRoots.from(files)
            reportUndocumented.set(false)
        }
    }
    
    val packageJavadoc by tasks.creating(Jar::class) {
        dependsOn("dokkaGenerateHtml")
        from("$buildDir/javadoc")
        archiveClassifier.set("javadoc")
    }

    return packageJavadoc
}