import com.jetbrains.rd.gradle.dependencies.kotlinVersion
import com.jetbrains.rd.gradle.tasks.CopySourcesTask
import com.jetbrains.rd.gradle.tasks.util.*
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.utils.addToStdlib.cast
import javax.inject.Inject

typealias applyingConfiguration = Project.() -> Unit
extra["applyKotlinJVM"].cast<applyingConfiguration>().invoke(project)

plugins {
    kotlin("jvm")
}

dependencies {
    compile(project(":rd-core:"))
    implementation(gradleApi())
    testCompile(project(":rd-framework"))
    compile("org.jetbrains.kotlin:kotlin-compiler:${kotlinVersion}")
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

            output.dir(rootProject.buildDir
                    .resolve("models")
                    .resolve("interning")
            )
            output.dir(rootProject.buildDir
                    .resolve("models")
                    .resolve("demo"))
            compiledBy("generateEverything")
        }
    }
    test {
        compiledBy("testCopySources")
    }
}

val testCopySources by tasks.creating(CopySourcesTask::class) {
    currentSourceSet = sourceSets.test.get()
    currentProject = project
    generativeSourceSet = models
}

tasks.named("compileTestKotlin") {
    dependsOn(testCopySources)
}

val modelsImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.implementation.get())
}

//configurations["modelsCompileOnly"].extendsFrom(configurations.compileOnly.get())

dependencies {
    testCompile(sourceSets["models"].output)
}
