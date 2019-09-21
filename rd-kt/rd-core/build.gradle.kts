import com.jetbrains.rd.gradle.dependencies.kotlinVersion
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.gradle.kotlin.dsl.kotlin

plugins {
    kotlin("multiplatform")
}

typealias applyingConfiguration = Project.() -> Unit
extra["applyMultiplatform"].cast<applyingConfiguration>().invoke(project)

kotlin {
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation("commons-logging:commons-logging:1.2")
            }
        }
    }
}
