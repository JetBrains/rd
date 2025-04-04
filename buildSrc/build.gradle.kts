repositories {
    mavenCentral()
    gradlePluginPortal()
    maven { url = uri("https://packages.jetbrains.team/maven/p/jcs/maven") }
}

plugins {
    `kotlin-dsl`
    signing
}

dependencies {
    implementation(gradleApi())
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.7.0")
    implementation("com.moowork.gradle:gradle-node-plugin:1.3.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("com.jetbrains:jet-sign:45.47")
}
