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
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:2.0.0")
    implementation("com.moowork.gradle:gradle-node-plugin:1.3.1")
    implementation("com.jetbrains:jet-sign:45.47")
}
