import com.jetbrains.rd.gradle.plugins.applyKotlinJVM

applyKotlinJVM()

plugins {
    kotlin("jvm")
}

repositories {
    maven {
        url = uri("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
    }
}

dependencies {
    implementation(project(":rd-framework"))
    testImplementation("org.jetbrains:jetCheck:0.2.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:${com.jetbrains.rd.gradle.dependencies.kotlinVersion}")
}

publishing.publications.named<MavenPublication>("pluginMaven") {
    pom {
        name.set("rd-text")
        description.set("Efficient text communication through the RD protocol.")
    }
}

