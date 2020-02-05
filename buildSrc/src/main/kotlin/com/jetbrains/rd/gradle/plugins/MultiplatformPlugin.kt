package com.jetbrains.rd.gradle.plugins

import com.jetbrains.rd.gradle.dependencies.junitVersion
import com.jetbrains.rd.gradle.dependencies.kotlinxCoroutinesVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.api.publish.tasks.GenerateModuleMetadata
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

fun Project.applyMultiplatform() = apply<MultiplatformPlugin>()

@Suppress("UNUSED_VARIABLE")
open class MultiplatformPlugin : Plugin<Project> {
    override fun apply(target: Project) = target.run {
        group = "com.jetbrains.rd"
        version = rootProject.version

        apply(plugin = "org.jetbrains.kotlin.multiplatform")
        apply(plugin = "maven-publish")
        apply(plugin = "org.jetbrains.dokka")

        configure<KotlinMultiplatformExtension> {
            val packageJavadoc = createPackageJavaDoc(files("src/commonMain/kotlin", "src/jvmMain/kotlin"))

            jvm {}
            js {}
            metadata {}

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
                        implementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
                        runtimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
                        implementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
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

            tasks.withType<Test> {
                useJUnitPlatform()
            }

            tasks.withType<GenerateModuleMetadata> {
                enabled = false
            }

            tasks.withType<AbstractPublishToMaven>()
                .matching {
                    it.name.startsWith("publishKotlinMultiplatformPublication")
                }
                .all {
                    enabled = false
                }

            configure<PublishingExtension> {
                publications.withType<MavenPublication>().apply {
                    val jvm by getting {
                        artifactId = project.name

                        artifact(packageJavadoc)
                    }
                    val metadata by getting {
                        artifactId = project.name + "-common"

                        artifact(packageJavadoc)
                    }
                }

                setRemoteRepositories()
            }
        }
    }
}

