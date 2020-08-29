repositories {
    mavenLocal()
    mavenCentral()
    jcenter()
    maven { setUrl("https://plugins.gradle.org/m2/") }
    gradlePluginPortal()
    maven {
        url = uri("https://dl.bintray.com/kotlin/kotlin-eap")
    }
}

plugins {
    `kotlin-dsl`
    jacoco
//    `java-library`
    kotlin("multiplatform") version "1.4.0" apply false
}

dependencies {
    implementation(gradleApi())
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:0.9.18")
//    implementation("org.gradle.kotlin:plugins:1.3.1")
    implementation("com.moowork.gradle:gradle-node-plugin:1.3.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
}
