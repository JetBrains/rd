repositories {
    mavenLocal()
    mavenCentral()
    jcenter()
    maven { setUrl("https://plugins.gradle.org/m2/") }
    gradlePluginPortal()
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-stdlib:1.4.0")
        force("org.jetbrains.kotlin:kotlin-stdlib-common:1.4.0")
        force("org.jetbrains.kotlin:kotlin-reflect:1.4.0")
        force("org.jetbrains.kotlin:kotlin-runtime:1.4.0")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.4.0")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.4.0")
        force("org.jetbrains.kotlin:kotlin-stdlib-js:1.4.0")
        force("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.4.0")
        force("org.jetbrains.kotlin:kotlin-script-runtime:1.4.0")
    }
}

sourceSets {
    main {
        compileClasspath = configurations.compileClasspath.get().minus(files(gradle.gradleHomeDir?.resolve("lib")?.listFiles()?.filter { it.name.contains("kotlin-stdlib") || it.name.contains("kotlin-reflect") } ?: listOf<File>()))
    }
}

plugins {
    `kotlin-dsl`
    jacoco
    kotlin("multiplatform") version "1.4.0" apply false
}

dependencies {
    implementation(gradleApi())
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:0.9.18")
    implementation("com.moowork.gradle:gradle-node-plugin:1.3.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
}
