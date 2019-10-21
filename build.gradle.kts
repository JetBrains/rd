import com.jetbrains.rd.gradle.dependencies.kotlinVersion

version = System.getenv("RELEASE_VERSION_NUMBER") ?: "SNAPSHOT"

buildscript {
    project.extra.apply {
        val repoRoot = rootProject.projectDir
        set("repoRoot", repoRoot)
        set("cppRoot", File(repoRoot, "rd-cpp"))
        set("ktRoot", File(repoRoot, "rd-kt"))
        set("csRoot", File(repoRoot, "rd-net"))
    }
}

repositories {
}

plugins {
    base
}

allprojects {
    plugins.apply("maven-publish")

    configurations.all {
        resolutionStrategy {
            force("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
            force("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
            force("org.jetbrains.kotlin:kotlin-runtime:$kotlinVersion")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
            force("org.jetbrains.kotlin:kotlin-stdlib-js:$kotlinVersion")
        }
    }

    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
        maven { setUrl("https://jitpack.io") }
    }
}

val clean by tasks.getting(Delete::class) {
    delete(rootProject.buildDir)
}