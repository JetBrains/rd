package com.jetbrains.rd.gradle.tasks

import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskAction


open class KotlinExec : JavaExec() {

    @TaskAction
    override fun exec() {
        super.exec()
    }

    fun addDependencies() {
        classpath += project.files("../rd-kt/rd-framework/build/classes/kotlin/jvm/test")
        main = "com.jetbrains.rd.framework.test.cross." + name + "Kt"

        args(tmpFilePath)
    }
}