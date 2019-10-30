@file:Suppress("UNUSED_VARIABLE", "LocalVariableName")

import com.jetbrains.rd.gradle.tasks.DotnetBuildTask
import com.jetbrains.rd.gradle.tasks.DotnetRunTask


tasks {
    val build by creating(DotnetBuildTask::class) {
        configuration("Debug")
    }

    val buildCrossTests by creating(DotnetBuildTask::class) {
        dependsOn(":rd-gen:generateEverything")

        configuration("CrossTests")
    }

    val clean by creating(Exec::class) {
        executable = "dotnet"
        args = listOf("clean")
    }

    fun creatingCrossTestTask() = creating(DotnetRunTask::class) {
        dependsOn(buildCrossTests)
        workingDir = workingDir.resolve("Test.Cross")
    }

    val CrossTestCsClientAllEntities by creatingCrossTestTask()

    val CrossTestCsServerAllEntities by creatingCrossTestTask()

    val CrossTestCsClientBigBuffer by creatingCrossTestTask()

    val CrossTestCsClientRdCall by creatingCrossTestTask()
}