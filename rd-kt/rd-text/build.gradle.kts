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
}

