@file:Suppress("UNUSED_VARIABLE", "LocalVariableName")

import com.jetbrains.rd.gradle.tasks.DotnetBuildTask
import com.jetbrains.rd.gradle.tasks.DotnetRunTask
import com.jetbrains.rd.gradle.tasks.RunScriptTask


tasks {
    val build by creating(DotnetBuildTask::class) {
        dependsOn(":rd-gen:generateEverything")
    }

    val clean by creating(Exec::class) {
        executable = "dotnet"
        args = listOf("clean")
    }

    fun creatingCrossTestTask() = creating(DotnetRunTask::class) {
        dependsOn(build)
        workingDir = workingDir.resolve("CrossTest")
    }

    val CrossTestCsClientAllEntities by creatingCrossTestTask()

    val CrossTestCsClientBigBuffer by creatingCrossTestTask()

    val CrossTestCsClientRdCall by creatingCrossTestTask()
}