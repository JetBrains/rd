package com.jetbrains.rd.gradle.tasks

import org.gradle.api.tasks.Exec
import java.io.File

open class DotnetBuildTask : Exec() {
    private lateinit var _projectName: String

    var projectName: String
        get() = _projectName
        set(value) {
            _projectName = value
            executable = "dotnet"
            workingDir = File(workingDir, "Cross")
            args = listOf("build", projectName, "-p:Configuration=Release", "--output=build")
        }


    init {
        group = "dotnet build"
    }

    public override fun exec() {
        println(commandLine)
        super.exec()
    }
}