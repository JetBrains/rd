import com.jetbrains.rd.gradle.plugins.applyKotlinJVM
import com.jetbrains.rd.gradle.tasks.creatingCopySourcesTask

applyKotlinJVM()

plugins {
    kotlin("jvm")
}

kotlin {
    sourceSets {
        main {
            dependencies {
                api(project(":rd-core"))
            }
        }
    }
}

val testCopySources by creatingCopySourcesTask(
        kotlin.sourceSets.test,
        evaluationDependsOn(":rd-gen").sourceSets["models"]
)

tasks.named("compileTestKotlin") {
    dependsOn(testCopySources)
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${com.jetbrains.rd.gradle.dependencies.kotlinxCoroutinesVersion}")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:${com.jetbrains.rd.gradle.dependencies.kotlinVersion}")
}
