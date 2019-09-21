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
//    kotlin("multiplatform") version "1.3.50"
}

dependencies {
    implementation(gradleApi())
    compileOnly(gradleApi())
    runtimeOnly(gradleApi())
}