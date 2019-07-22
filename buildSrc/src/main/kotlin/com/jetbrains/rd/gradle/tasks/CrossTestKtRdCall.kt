package com.jetbrains.rd.gradle.tasks

import org.gradle.api.tasks.TaskAction

open class CrossTestKtRdCall : KotlinExec() {
    init {
        classpath += project.rootProject.files("rd-kt/rd-framework/build/classes/kotlin/jvm/test")
        main = "com.jetbrains.rd.framework.test.cross.${name}Kt"
    }

    @TaskAction
    override fun exec() {
        args = args?.plus(tmpFile.absolutePath)
        super.exec()
    }
}