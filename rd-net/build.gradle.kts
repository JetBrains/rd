@file:Suppress("UNUSED_VARIABLE", "LocalVariableName")

import com.jetbrains.rd.gradle.tasks.DotnetRunTask
import com.jetbrains.rd.gradle.tasks.RunScriptTask


tasks {
    val build by creating(Exec::class) {
        dependsOn(":rd-gen:generateEverything")

        executable = "dotnet"
        args = listOf("build")
    }

    val clean by creating(Exec::class) {
        executable = "dotnet"
        args = listOf("clean")
    }

    val CrossTestCsClientAllEntities by creating(DotnetRunTask::class)

    val CrossTestCsClientBigBuffer by creating(DotnetRunTask::class)

    val CrossTestCsClientRdCall by creating(DotnetRunTask::class)
}