import com.jetbrains.rd.gradle.plugins.applyKotlinJVM

applyKotlinJVM()

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":rd-core"))
}

publishing.publications.named<MavenPublication>("pluginMaven") {
    pom {
        name.set("rd-swing")
        description.set("Utilities for integration of Swing UI framework with the reactive entities.")
    }
}
