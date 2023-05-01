package com.jetbrains.rd.gradle.plugins

import com.jetbrains.rd.gradle.dependencies.junitVersion
import com.jetbrains.rd.gradle.dependencies.kotlinVersion
import jetbrains.sign.GpgSignSignatoryProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.tasks.GenerateModuleMetadata
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun Project.applyKotlinJVM() = apply<KotlinJVMPlugin>()

open class KotlinJVMPlugin : Plugin<Project> {
    override fun apply(target: Project) = target.run {
        apply(plugin = "java")
        apply(plugin = "maven-publish")
        apply(plugin = "org.jetbrains.dokka")
        apply(plugin = "signing")

        configure<KotlinJvmProjectExtension> {
            val sourceJar by tasks.creating(Jar::class) {
                from(sourceSets["main"].kotlin.sourceDirectories)
                archiveClassifier.set("sources")
            }

            val packageJavadoc = createPackageJavaDoc(files("src/main/kotlin"))

            tasks {
                named<KotlinCompile>("compileKotlin") {
                    kotlinOptions {
                        jvmTarget = "17"
                    }
                }

                named<KotlinCompile>("compileTestKotlin") {
                    kotlinOptions {
                        jvmTarget = "17"
                    }
                }
            }

            tasks.withType<GenerateModuleMetadata> {
                enabled = false
            }

            configure<PublishingExtension> {
                publications {
                    if (project.name != "rd-cross") {
                        register("pluginMaven", MavenPublication::class.java) {
                            groupId = "com.jetbrains.rd"
                            artifactId = project.name
                            version = rootProject.version as String

                            pom {
                                url.set("https://github.com/JetBrains/rd")
                                licenses {
                                    license {
                                        name.set("The Apache License, Version 2.0")
                                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                                    }
                                }
                                developers {
                                    // According to the reference, this should be the person(s) to be contacted about the project.
                                    developer {
                                        id.set("ivan.migalev")
                                        name.set("Ivan Migalev")
                                        email.set("ivan.migalev@jetbrains.com")
                                    }
                                    developer {
                                        id.set("mikhail.filippov")
                                        name.set("Mikhail Filippov")
                                        email.set("Mikhail.Filippov@jetbrains.com")
                                    }
                                }
                                scm {
                                    connection.set("scm:git:https://github.com/JetBrains/rd.git")
                                    url.set("https://github.com/JetBrains/rd")
                                }
                            }

                            from(components["kotlin"])

                            artifact(sourceJar)
                            artifact(packageJavadoc)
                        }
                    }
                }

                val isUnderTeamCity = System.getenv("TEAMCITY_VERSION") != null
                project.configure<SigningExtension> {
                    if (isUnderTeamCity) {
                        sign(publications)
                        signatories = GpgSignSignatoryProvider()
                    }
                }
                val deployToProduction = rootProject.extra["deployMavenToProduction"].toString().toBoolean()
                repositories {
                    maven {
                        name = "artifacts"
                        url = uri(rootProject.projectDir.resolve("build").resolve("artifacts").resolve("maven"))
                    }
                    if (deployToProduction) {
                        maven {
                            name = "maven-central"
                            url = uri("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies/")
                            credentials {
                                username = "Bearer"
                                password = rootProject.extra["internalNuGetFeedKey"].toString()
                            }
                        }
                    }
                }
            }


            val test by tasks.getting(Test::class) {
                maxHeapSize = "512m"
                useJUnitPlatform()
            }

            dependencies {
                "implementation"("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
                "implementation"("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
                "implementation"("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

                "testImplementation"("org.junit.jupiter:junit-jupiter-api:$junitVersion")
                "testRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
                "testImplementation"("org.junit.jupiter:junit-jupiter-params:$junitVersion")
                "testImplementation"("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
            }
        }
    }
}
