package com.jetbrains.rd.gradle.plugins

import com.jetbrains.rd.gradle.dependencies.junitVersion
import com.jetbrains.rd.gradle.dependencies.kotlinxCoroutinesVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

fun Project.applyMultiplatform() = apply<MultiplatformPlugin>()

@Suppress("UNUSED_VARIABLE")
open class MultiplatformPlugin : Plugin<Project> {
    override fun apply(target: Project) = target.run {
        group = "com.jetbrains.rd"
        version = System.getenv("RELEASE_VERSION_NUMBER") ?: "SNAPSHOT"

        apply(plugin = "org.jetbrains.kotlin.multiplatform")
        apply(plugin = "maven-publish")
        apply(plugin = "org.jetbrains.dokka")

        configure<KotlinMultiplatformExtension> {
            jvm {
                mavenPublication {
                    artifactId = project.name
                }
            }

            js {

            }

            metadata {
                mavenPublication {
                    artifactId = project.name + "-common"
                }
            }


            sourceSets {
                val commonMain by getting {
                    dependencies {
                        implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
                        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
                    }
                }
                val commonTest by getting {
                    dependencies {
                        implementation("org.jetbrains.kotlin:kotlin-test-common")
                        implementation("org.jetbrains.kotlin:kotlin-test-annotations-common")
                    }

                }
                val jvmMain by getting {
                    dependencies {
                        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
                        implementation("org.jetbrains.kotlin:kotlin-reflect")
                    }
                }
                val jvmTest by getting {
                    dependencies {
                        implementation("junit:junit:$junitVersion")
                        implementation("org.jetbrains.kotlin:kotlin-test")
                        implementation("org.jetbrains.kotlin:kotlin-test-junit")
                    }
                }
                val jsMain by getting {
                    dependencies {
                        implementation("org.jetbrains.kotlin:kotlin-stdlib-js")
                    }
                }
                val jsTest by getting {
                    dependencies {
                        implementation("org.jetbrains.kotlin:kotlin-test-js")
                    }
                }
            }
        }

        tasks {
            val dokka by getting(DokkaTask::class) {
                outputFormat = "html"
                outputDirectory = "$buildDir/javadoc"
                kotlinTasks {
                    emptyList()
                }
                sourceDirs = files("src/commonMain/kotlin", "src/jvmMain/kotlin")
            }

            val packageJavadoc = create<Jar>("packageJavadoc") {
                dependsOn(dokka)
                from("$buildDir/javadoc")
                archiveClassifier.set("javadoc")
            }
        }

        configure<PublishingExtension> {
            publications {
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
    }
}

