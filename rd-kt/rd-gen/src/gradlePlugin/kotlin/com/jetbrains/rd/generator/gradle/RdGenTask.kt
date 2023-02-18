package com.jetbrains.rd.generator.gradle

import org.gradle.api.tasks.JavaExec
import java.io.File

open class RdGenTask : JavaExec() {
    private val local = extensions.create("params", RdGenExtension::class.java, this)
    private val global = project.extensions.findByType(RdGenExtension::class.java)

    fun rdGenOptions(action: (RdGenExtension) -> Unit) {
        local.apply(action)
    }

    private val effectiveParams: RdGenExtension
        get() = local.mergeWith(global!!)

    override fun exec() {
        val params = effectiveParams
        args(params.toArguments())

        val files = project.configurations.getByName("rdGenConfiguration").files
        val buildScriptFiles = project.buildscript.configurations.getByName("classpath").files
        val rdFiles: MutableSet<File> = HashSet()
        for (file in buildScriptFiles) {
            if (file.name.contains("rd-")) {
                rdFiles.add(file)
            }
        }
        classpath(files)
        classpath(rdFiles)
        try {
            super.exec()
        } finally {
            cleanup(params)
        }
    }

    init {
        mainClass.set("com.jetbrains.rd.generator.nova.MainKt")
    }

    private fun cleanup(params: RdGenExtension) {
        params.tempGeneratorsFile?.delete()
    }
}
