import com.jetbrains.rd.gradle.plugins.applyCrossTest
import com.jetbrains.rd.gradle.plugins.applyKotlinJVM
import com.jetbrains.rd.gradle.tasks.CopySourcesTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

applyKotlinJVM()
applyCrossTest()

plugins {
    kotlin("jvm")
}

val testCopySources by tasks.creating(CopySourcesTask::class) {
    dependsOn(":rd-gen:generateEverything")
    currentSourceSet = kotlin.sourceSets.main.get()
    currentProject = project
    generativeSourceSet = evaluationDependsOn(":rd-gen").sourceSets["models"]

    lateInit()
}

tasks {
    withType<KotlinCompile> {
        dependsOn(testCopySources)
    }

//    test { onlyIf { !project.hasProperty("TEAMCITY_VERSION") } }
}
