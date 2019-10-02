import com.jetbrains.rd.gradle.tasks.RunScriptTask


tasks {
    val build by creating(RunScriptTask::class) {
        execPath = "build.cmd"
    }

    val clean by creating(Exec::class) {
        executable = "dotnet"
        args = listOf("clean")
    }
}