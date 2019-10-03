import com.jetbrains.rd.gradle.plugins.applyMultiplatform
import com.jetbrains.rd.gradle.tasks.CopySourcesTask

plugins {
    kotlin("multiplatform")
}

repositories {

}

applyMultiplatform()

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":rd-core"))
            }
        }
        commonTest {

        }

    }
}


val testCopySources by tasks.creating(CopySourcesTask::class) {
    dependsOn(":rd-gen:generateEverything")
    currentSourceSet = kotlin.sourceSets.commonTest.get()
    currentProject = project
    generativeSourceSet = evaluationDependsOn(":rd-gen").sourceSets["models"]

    lateInit()
}

tasks.named("compileTestKotlinJvm") {
    dependsOn(testCopySources)
}
