repositories {
    mavenCentral()
    gradlePluginPortal()
    maven { url = uri("https://packages.jetbrains.team/maven/p/jcs/maven") }
}

plugins {
    `kotlin-dsl`
    jacoco
    signing
    kotlin("multiplatform") version "1.4.0" apply false
}

dependencies {
    implementation(gradleApi())
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:0.9.18")
    implementation("com.moowork.gradle:gradle-node-plugin:1.3.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
    implementation("com.jetbrains:jet-sign:45.47")
}
