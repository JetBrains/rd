import com.jetbrains.rd.gradle.plugins.applyKotlinJVM

applyKotlinJVM()

plugins {
    kotlin("jvm")
}

dependencies {
    implementation("commons-logging:commons-logging:1.3.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${com.jetbrains.rd.gradle.dependencies.kotlinxCoroutinesVersion}")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:${com.jetbrains.rd.gradle.dependencies.kotlinVersion}")
}

publishing.publications.named<MavenPublication>("pluginMaven") {
    pom {
        name.set("rd-core")
        description.set("Core reactive programming utilities.")
    }
}
