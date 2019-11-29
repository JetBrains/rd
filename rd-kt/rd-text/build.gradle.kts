import com.jetbrains.rd.gradle.plugins.applyKotlinJVM
import org.jetbrains.kotlin.utils.addToStdlib.cast

applyKotlinJVM()

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":rd-framework"))

    testImplementation("com.github.JetBrains:jetCheck:b5bc810e71")
    testImplementation("com.github.JetBrains:jetCheck:b5bc810e71:sources")
}

