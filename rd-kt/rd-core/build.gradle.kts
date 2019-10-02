import com.jetbrains.rd.gradle.dependencies.kotlinVersion
import com.jetbrains.rd.gradle.plugins.applyMultiplatform
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.gradle.kotlin.dsl.kotlin

plugins {
    kotlin("multiplatform")
}

applyMultiplatform()

kotlin {
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation("commons-logging:commons-logging:1.2")
            }
        }
    }
}
