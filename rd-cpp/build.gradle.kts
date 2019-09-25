import com.jetbrains.rd.gradle.tasks.CrossTestCppTask
import com.jetbrains.rd.gradle.tasks.RunScriptTask
import com.jetbrains.rd.gradle.tasks.RunExecTask

/*tasks {
    val build by creating(RunScriptTask::class) {
        execPath = "build.cmd"
    }

    val buildTests by creating(RunScriptTask::class) {
        dependsOn(":rd-gen:generateEverything")
        execPath = "buildtest.cmd"
    }

    val clean by creating(Delete::class) {
        delete("$project.buildDir")
    }

    val CrossTestCppClientAllEntities by creating(CrossTestCppTask::class) {
        dependsOn("buildTests")
    }

    val CrossTestCppClientBigBuffer by creating(CrossTestCppTask::class) {
        dependsOn("buildTests")
    }

    val CrossTestCppClientRdCall by creating(CrossTestCppTask::class) {
        dependsOn("buildTests")
    }
}*/