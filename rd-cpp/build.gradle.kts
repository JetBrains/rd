import com.jetbrains.rd.gradle.tasks.RunScriptTask

tasks {
    val build by creating(RunScriptTask::class) {
        execPath = "build.cmd"
    }

    val buildTests by creating(RunScriptTask::class) {
        dependsOn(":rd-gen:generateEverything")
        execPath = "buildtest.cmd"
    }
}