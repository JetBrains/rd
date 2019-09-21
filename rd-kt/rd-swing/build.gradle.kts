import org.jetbrains.kotlin.utils.addToStdlib.cast

typealias applyingConfiguration = Project.() -> Unit
extra["applyKotlinJVM"].cast<applyingConfiguration>().invoke(project)

plugins {
    kotlin("jvm")
}

dependencies {
    compile(project(":rd-core"))
}
