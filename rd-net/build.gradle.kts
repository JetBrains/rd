@file:Suppress("UNUSED_VARIABLE", "LocalVariableName")

import com.jetbrains.rd.gradle.tasks.DotnetRunTask
import com.jetbrains.rd.gradle.tasks.RunScriptTask


tasks {
    val build by creating(RunScriptTask::class) {
        execPath = "build.cmd"
    }

    val clean by creating(Exec::class) {
        executable = "dotnet"
        args = listOf("clean")
    }

    val CrossTestCsClientAllEntities by creating(DotnetRunTask::class)

    val CrossTestCsClientBigBuffer by creating(DotnetRunTask::class)

    val CrossTestCsClientRdCall by creating(DotnetRunTask::class)
}