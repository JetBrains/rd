import com.jetbrains.rd.gradle.plugins.applyMultiplatform
import com.jetbrains.rd.gradle.tasks.creatingCopySourcesTask

plugins {
    kotlin("multiplatform")
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

val testCopySources by creatingCopySourcesTask(
        kotlin.sourceSets.commonTest,
        evaluationDependsOn(":rd-gen").sourceSets["models"]
)

tasks.named("compileTestKotlinJvm") {
    dependsOn(testCopySources)
}
