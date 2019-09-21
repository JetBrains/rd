import org.jetbrains.kotlin.utils.addToStdlib.cast

typealias applyingConfiguration = Project.() -> Unit
extra["applyKotlinJVM"].cast<applyingConfiguration>().invoke(project)

plugins {
    kotlin("jvm")
}

dependencies {
    compile(project(":rd-framework"))

    testCompile("com.github.JetBrains:jetCheck:b5bc810e71")
    testCompile("com.github.JetBrains:jetCheck:b5bc810e71:sources")
}

