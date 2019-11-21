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
    kotlin("multiplatform") version "1.3.50" apply false
}

dependencies {
    compile(gradleApi())
    compile("org.jetbrains.kotlin:kotlin-gradle-plugin")
    compile("org.jetbrains.dokka:dokka-gradle-plugin:0.9.18")
//    compile("org.gradle.kotlin:plugins:1.3.1")
    compile("com.moowork.gradle:gradle-node-plugin:1.3.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.1")
}