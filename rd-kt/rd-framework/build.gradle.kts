import com.jetbrains.rd.gradle.plugins.applyMultiplatform
import com.jetbrains.rd.gradle.tasks.creatingCopySourcesTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlin.Experimental"
}

val testCopySources by creatingCopySourcesTask(
        kotlin.sourceSets.commonTest,
        evaluationDependsOn(":rd-gen").sourceSets["models"]
)

tasks.named("compileTestKotlinJvm") {
    dependsOn(testCopySources)
}
