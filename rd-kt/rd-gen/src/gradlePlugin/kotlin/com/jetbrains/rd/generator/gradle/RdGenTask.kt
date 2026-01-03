package com.jetbrains.rd.generator.gradle

import org.gradle.api.tasks.JavaExec
import java.io.File
import java.nio.file.Files

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
        val tempGeneratorsFile =
            if (params.hasGenerators) Files.createTempFile("rd-", ".generators").toFile()
            else null
        try {
            args(params.toArguments(tempGeneratorsFile))

            val buildScriptFiles = project.buildscript.configurations.getByName("classpath").files
            val rdFiles: MutableSet<File> = HashSet()
            for (file in buildScriptFiles) {
                if (file.name.contains("rd-")) {
                    rdFiles.add(file)
                }
            }
            classpath(rdFiles)

            super.exec()
        } finally {
            tempGeneratorsFile?.delete()
        }
    }

    init {
        mainClass.set("com.jetbrains.rd.generator.nova.MainKt")
    }
}
