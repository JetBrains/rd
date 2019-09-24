package com.jetbrains.rd.gradle.plugins

import com.jetbrains.rd.gradle.dependencies.junitVersion
import com.jetbrains.rd.gradle.dependencies.kotlinVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.*
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun Project.applyKotlinJVM() = apply<KotlinJVMPlugin>()

open class KotlinJVMPlugin : Plugin<Project> {
    override fun apply(target: Project) = target.run {
        apply(plugin = "java")
        apply(plugin = "kotlin-platform-jvm")
        apply(plugin = "jacoco")
        apply(plugin = "maven-publish")
        apply(plugin = "org.jetbrains.dokka")

        configure<JacocoPluginExtension> {
            toolVersion = "0.8.2"
        }

        configure<KotlinJvmProjectExtension> {
            val sourceJar by tasks.creating(Jar::class) {
                from(sourceSets["main"].kotlin.sourceDirectories)
                archiveClassifier.set("sources")
            }

            val dokka by tasks.getting(DokkaTask::class) {
                outputFormat = "html"
                outputDirectory = "$buildDir/javadoc"
                kotlinTasks {
                    emptyList()
                }
                sourceDirs = files("src/main/kotlin")
            }

            val packageJavadoc by tasks.creating(Jar::class) {
                dependsOn(dokka)
                from("$buildDir/javadoc")
                archiveClassifier.set("javadoc")
            }

            tasks {
                named<KotlinCompile>("compileKotlin") {
                    kotlinOptions {
                        jvmTarget = "1.8"
                    }
                }

                named<KotlinCompile>("compileTestKotlin") {
                    kotlinOptions {
                        jvmTarget = "1.8"
                    }
                }
            }

            configure<PublishingExtension> {
                publications {
                    var mvnId = project.name
                    if (mvnId.endsWith("-jvm"))
                        mvnId = mvnId.dropLast(4)
                    else if (mvnId.endsWith("-core") || mvnId.endsWith("-framework"))
                        mvnId = "$mvnId-common"

                    register("maven", MavenPublication::class.java) {
                        groupId = "com.jetbrains.rd"
                        artifactId = mvnId
                        version = System.getenv("RELEASE_VERSION_NUMBER") ?: "SNAPSHOT"

                        from(components["kotlin"])

                        artifact(sourceJar)
                        artifact(packageJavadoc)
                    }

                    repositories {
                        maven {
                            setUrl("https://www.myget.org/F/rd-snapshots/maven/")
                            credentials {
                                username = System.getenv("MYGET_USERNAME")
                                password = System.getenv("MYGET_PASSWORD")
                            }
                        }
                    }
                }
            }


            val test by tasks.getting(Test::class) {
                maxHeapSize = "512m"
                finalizedBy(tasks.named("jacocoTestReport"))
            }

            dependencies {
                compile("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
                compile("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
                compile("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

                testCompile("junit:junit:$junitVersion")
                testCompile("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
                testCompile("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
            }
        }
    }
}