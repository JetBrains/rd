package com.jetbrains.rd.gradle.tasks

open class CrossTestKtTask : KotlinExec() {
    init {
        classpath += project.rootProject.files("rd-kt/rd-framework/build/classes/kotlin/jvm/test")
        main = "com.jetbrains.rd.framework.test.cross." + name + "Kt"
        args = args?.plus(tmpFilePath)
    }
}