package com.jetbrains.rd.gradle.plugins

import com.jetbrains.rd.gradle.dependencies.junitVersion
import com.jetbrains.rd.gradle.dependencies.kotlinVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.tasks.GenerateModuleMetadata
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.*
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun Project.applyKotlinJVM() = apply<KotlinJVMPlugin>()

@Suppress("UNUSED_VARIABLE")
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

            val packageJavadoc = createPackageJavaDoc(files("src/main/kotlin"))

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

            tasks.withType<GenerateModuleMetadata> {
                enabled = false
            }

            configure<PublishingExtension> {
                publications {
                    var mvnId = project.name
                    if (mvnId.endsWith("-jvm"))
                        mvnId = mvnId.dropLast(4)
                    else if (mvnId.endsWith("-core") || mvnId.endsWith("-framework"))
                        mvnId = "$mvnId-common"

                    register("pluginMaven", MavenPublication::class.java) {
                        groupId = "com.jetbrains.rd"
                        artifactId = mvnId
                        version = rootProject.version as String

                        from(components["kotlin"])

                        artifact(sourceJar)
                        artifact(packageJavadoc)
                    }

                    setRemoteRepositories()
                }
            }


            val test by tasks.getting(Test::class) {
                maxHeapSize = "512m"
                finalizedBy(tasks.named("jacocoTestReport"))
            }

            dependencies {
                "implementation"("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
                "implementation"("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
                "implementation"("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

                "testImplementation"("org.junit.jupiter:junit-jupiter-api:$junitVersion")
                "testRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
                "testImplementation"("org.junit.jupiter:junit-jupiter-params:$junitVersion")
                "testImplementation"("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
                "testImplementation"("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
            }
        }
    }
}
