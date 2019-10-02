import com.jetbrains.rd.gradle.plugins.applyKotlinJVM

applyKotlinJVM()

plugins {
    kotlin("jvm")
}

dependencies {
    compile(project(":rd-core"))
}
