@file:Suppress("UNUSED_VARIABLE", "LocalVariableName")

import com.jetbrains.rd.gradle.tasks.CppBuildTask
import com.jetbrains.rd.gradle.tasks.CrossTestCppTask

tasks {
    val build by creating(CppBuildTask::class) {
        execPath("build.cmd")
    }

    val buildTests by creating(CppBuildTask::class) {
        dependsOn(":rd-gen:generateEverything")
        execPath("buildtest.cmd")
    }

    val clean by creating(Delete::class) {
        delete("${project.buildDir}")
    }

    fun creatingCrossTestCppTask() = creating(CrossTestCppTask::class) {
        dependsOn(buildTests)
    }

    val CrossTestCppClientAllEntities by creatingCrossTestCppTask()

    val CrossTestCppClientBigBuffer by creatingCrossTestCppTask()

    val CrossTestCppClientRdCall by creatingCrossTestCppTask()
}
