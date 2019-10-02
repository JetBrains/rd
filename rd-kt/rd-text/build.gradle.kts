import com.jetbrains.rd.gradle.plugins.applyKotlinJVM
import org.jetbrains.kotlin.utils.addToStdlib.cast

applyKotlinJVM()

plugins {
    kotlin("jvm")
}

dependencies {
    compile(project(":rd-framework"))

    testCompile("com.github.JetBrains:jetCheck:b5bc810e71")
    testCompile("com.github.JetBrains:jetCheck:b5bc810e71:sources")
}

