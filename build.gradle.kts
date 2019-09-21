import com.jetbrains.rd.gradle.dependencies.junitVersion
import com.jetbrains.rd.gradle.dependencies.kotlinVersion
import com.jetbrains.rd.gradle.dependencies.kotlinxCoroutinesVersion
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

//group "com.jetbrains.rd"
version = System.getenv("RELEASE_VERSION_NUMBER") ?: "SNAPSHOT"

buildscript {
    apply(from = "versions.gradle.kts")

    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
        maven { setUrl("https://plugins.gradle.org/m2/") }
    }

/*
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("com.moowork.gradle:gradle-node-plugin:$nodeVersion")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:$dokkaVersion")
    }
*/
}

repositories {
    mavenCentral()
    jcenter()
}

plugins {
    kotlin("multiplatform")/* version "1.3.50"*/
    id("com.moowork.node") version "1.3.1"
//    `kotlin-dsl`
    `java-gradle-plugin`
    java
    jacoco/* apply false*/ //> Plugin 'org.gradle.jacoco' is a core Gradle plugin, which is already on the classpath.
    id("org.jetbrains.dokka")/* version "0.9.18" */apply false
}


allprojects {
    plugins.apply("maven-publish")

    ext["applyKotlinJS"] = { project: Project ->
        project.also {
            apply(plugin = "kotlin-platform-js")
            apply(plugin = "maven-publish")

            dependencies {
                compile("org.jetbrains.kotlin:kotlin-stdlib-js:$kotlinVersion")
                testCompile("org.jetbrains.kotlin:kotlin-test-js:$kotlinVersion")
            }

            val target = "${projectDir}/build/classes/main"
            val projectName = name

            tasks {
                val sourcesJar = create<Jar>("sourcesJar") {
                    from(sourceSets.main.get().allSource)
                }

                val compileKotlin2Js by getting(Kotlin2JsCompile::class)
                compileKotlin2Js.apply {
                    kotlinOptions.metaInfo = true
                    kotlinOptions.outputFile = "$target/${projectName}.js"
                    kotlinOptions.sourceMap = true
                    kotlinOptions.moduleKind = "commonjs"
                    kotlinOptions.main = "call"
                    kotlinOptions.typedArrays = true
                }

                val compileTestKotlin2Js by getting(Kotlin2JsCompile::class)
                compileTestKotlin2Js.apply {
                    kotlinOptions.metaInfo = true
                    kotlinOptions.outputFile = "$target/${projectName}.test.js"
                    kotlinOptions.sourceMap = true
                    kotlinOptions.moduleKind = "commonjs"
                    kotlinOptions.main = "call"
                    kotlinOptions.typedArrays = true
                }
            }

            kotlin {
                experimental.coroutines = Coroutines.ENABLE
            }
        }
    }



/*
    ext["applyKotlinJVM"] = { project: Project ->
        project.also {
            apply(plugin = "java")
            apply(plugin = "kotlin-platform-jvm")
            apply(plugin = "jacoco")
            apply(plugin = "maven-publish")
            apply(plugin = "org.jetbrains.dokka")

            jacoco {
                toolVersion = "0.8.2"
            }

            val sourceJar = tasks.create<Jar>("sourceJar") {
                from(sourceSets["main"])
                archiveClassifier.set("sources")
            }

            val dokka by tasks.getting(DokkaTask::class) {
                outputFormat = "html"
                outputDirectory = "$buildDir/javadoc"
            }


            val packageJavadoc = tasks.create<Jar>("packageJavadoc") {
                dependsOn(dokka)
                from("$buildDir/javadoc")
                archiveClassifier.set("javadoc")
            }

            tasks.named<KotlinCompile>("compileKotlin") {
                kotlinOptions {
                    jvmTarget = "1.8"
                }
            }

            tasks.named<KotlinCompile>("compileTestKotlin") {
                kotlinOptions {
                    jvmTarget = "1.8"
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

                        from(components["java"])

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


            tasks.test {
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
*/

    ext["configureNodePlugin"] = {
        apply(plugin = "com.moowork.node")

        node {

            // Version of node to use.
            version = "8.7.0"

            // Version of npm to use.
            //npmVersion = "3.10.8"

            // Version of yarn to use.
            yarnVersion = "1.2.1"

            // Base URL for fetching node distributions (change if you have a mirror).
            distBaseUrl = "https://nodejs.org/dist"

            // If true, it will download node using above parameters.
            // If false, it will try to use globally installed node.
            download = true

            // Set the work directory for unpacking node
            workDir = file("${rootProject.buildDir}/nodejs")

            // Set the work directory where node_modules should be located
            nodeModulesDir = file("${rootProject.projectDir}")
        }
    }

/*
    ext["applyMultiplatform"] = { project: Project ->
        project.also {
            group = "com.jetbrains.rd"
            version = System.getenv("RELEASE_VERSION_NUMBER") ?: "SNAPSHOT"

            apply(plugin = "org.jetbrains.kotlin.multiplatform")
            apply(plugin = "maven-publish")
            apply(plugin = "org.jetbrains.dokka")


            kotlin {
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
                    commonMain {
                        dependencies {
                            implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
                            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
                        }
                    }
                    commonTest {
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
//                    kotlinTasks { }
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
*/

    configurations.all {
        resolutionStrategy {
            force("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
            force("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
            force("org.jetbrains.kotlin:kotlin-runtime:$kotlinVersion")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
            force("org.jetbrains.kotlin:kotlin-stdlib-js:$kotlinVersion")
        }
    }

    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
        maven { setUrl("https://jitpack.io") }
    }
}
