import com.jetbrains.rd.gradle.dependencies.kotlinVersion
import com.jetbrains.rd.gradle.plugins.applyKotlinJVM
import com.jetbrains.rd.gradle.tasks.creatingCopySourcesTask
import org.gradle.jvm.tasks.Jar

applyKotlinJVM()

plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":rd-core:"))
    implementation(gradleApi())
    implementation("org.jetbrains.intellij.deps:trove4j:1.0.20181211")
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable")
    implementation("org.jetbrains.kotlin:kotlin-script-runtime")
    testImplementation(project(":rd-framework"))
}

val fatJar = task<Jar>("fatJar") {
    manifest {
        attributes["Main-Class"] = "com.jetbrains.rd.generator.nova.MainKt"
    }
    archiveBaseName.set("rd")
    from(Callable { configurations.compile.get().map { if (it.isDirectory) it else zipTree(it) } })
    with(tasks["jar"] as CopySpec)
}

apply(from = "models.gradle.kts")

lateinit var models: SourceSet

sourceSets {
    main {
        compileClasspath = configurations.compileClasspath.get().minus(files(gradle.gradleHomeDir?.resolve("lib")?.listFiles()?.filter { it.name.contains("kotlin-stdlib") || it.name.contains("kotlin-reflect") } ?: listOf<File>()))
    }

    models = create("models") {
        kotlin {
            compileClasspath = configurations.compileClasspath.get().minus(files(gradle.gradleHomeDir?.resolve("lib")?.listFiles()?.filter { it.name.contains("kotlin-stdlib") || it.name.contains("kotlin-reflect") } ?: listOf<File>()))
            compileClasspath += main.get().output

            listOf("interning", "demo", "sync", "openEntity").map {
                rootProject.buildDir.resolve("models").resolve(it)
            }.forEach {
                output.dir(it)
            }

            compiledBy("generateEverything")
        }
    }
}

val testCopySources by creatingCopySourcesTask(kotlin.sourceSets.test, models)

tasks.named("compileTestKotlin") {
    dependsOn(testCopySources)
}

val modelsImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.implementation.get())
}
