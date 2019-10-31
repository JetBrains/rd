@file:Suppress("UNUSED_VARIABLE", "LocalVariableName")

import com.jetbrains.rd.gradle.tasks.CrossTestCsTask
import com.jetbrains.rd.gradle.tasks.DotnetBuildTask


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

    fun creatingCrossTestCsTask() = creating(CrossTestCsTask::class) {
        dependsOn(buildCrossTests)
        workingDir = workingDir.resolve("Test.Cross")
    }

    val CrossTest_AllEntities_CsClient by creatingCrossTestCsTask()

    val CrossTest_AllEntities_CsServer by creatingCrossTestCsTask()

    val CrossTest_BigBuffer_CsClient by creatingCrossTestCsTask()

    val CrossTest_RdCall_CsClient by creatingCrossTestCsTask()
}