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
    testImplementation(project(":rd-framework"))
    implementation("org.jetbrains.kotlin:kotlin-compiler:${kotlinVersion}")
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
    models = create("models") {
        kotlin {
            compileClasspath += main.get().output

            listOf("interning", "demo", "sync").map {
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
