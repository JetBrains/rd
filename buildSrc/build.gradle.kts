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
    `java`
    id("org.jetbrains.dokka") version "0.9.18"
    kotlin("multiplatform") version "1.3.50" apply false
    kotlin("jvm") version "1.3.50"
}

dependencies {
    implementation(gradleApi())
    compile(gradleApi())
//    runtimeOnly(gradleApi())
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
    compile("org.jetbrains.kotlin:kotlin-gradle-plugin")
//    implementation("org.jetbrains.dokka:dokka-gradle-plugin")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:0.9.18")
    compile("org.jetbrains.dokka:dokka-gradle-plugin:0.9.18")

}