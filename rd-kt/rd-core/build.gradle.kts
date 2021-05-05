import com.jetbrains.rd.gradle.plugins.applyMultiplatform

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
