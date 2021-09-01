import com.jetbrains.rd.gradle.plugins.applyKotlinJVM

applyKotlinJVM()

plugins {
    kotlin("jvm")
}

dependencies {
    implementation("commons-logging:commons-logging:1.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${com.jetbrains.rd.gradle.dependencies.kotlinxCoroutinesVersion}")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:${com.jetbrains.rd.gradle.dependencies.kotlinVersion}")
}