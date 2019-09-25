/*
@file:Suppress("LocalVariableName", "UNUSED_VARIABLE")

import com.jetbrains.rd.gradle.tasks.CrossTestKtRdTask
import com.jetbrains.rd.gradle.tasks.InteropTask

plugins {
    kotlin("jvm")
}

dependencies {
    compile(project(":rd-framework"))
    implementation(project(":rd-cpp"))
    implementation(project(":rd-net"))
    implementation(gradleApi())
}

fun getCppTaskByName(name: String): Task {
    return project(":rd-cpp").tasks.getByName(name)
}

fun getCsTaskByName(name: String): Task {
    return project(":rd-net").tasks.getByName(name)
}

tasks {
    //region Kt

    fun CrossTestKtRdTask.initialize() {
        dependsOn(":rd-framework:jvmTestClasses")
        classpath += project.sourceSets.test.get().runtimeClasspath
        classpath += project.sourceSets.test.get().compileClasspath
    }

    val CrossTestKtServerAllEntities by creating(CrossTestKtRdTask::class) {
        initialize()
    }

    val CrossTestKtServerBigBuffer by creating(CrossTestKtRdTask::class) {
        dependsOn(":rd-framework:jvmTestClasses")
        classpath += project.sourceSets.test.get().runtimeClasspath
        classpath += project.sourceSets.test.get().compileClasspath
    }

    val CrossTestKtServerRdCall by creating(CrossTestKtRdTask::class) {
        dependsOn(":rd-framework:jvmTestClasses")
        classpath += project.sourceSets.test.get().runtimeClasspath
        classpath += project.sourceSets.test.get().compileClasspath
    }
//endregion

//region KtCpp
    val CrossTestKtCppAllEntities by creating(InteropTask::class) {
        taskServer = CrossTestKtServerAllEntities
        taskClient = getCppTaskByName("CrossTestCppClientAllEntities")

        addDependencies()
    }

    val CrossTestKtCppBigBuffer by creating(InteropTask::class) {
        taskServer = CrossTestKtServerBigBuffer
        taskClient = getCppTaskByName("CrossTestCppClientBigBuffer")

        addDependencies()
    }

    val CrossTestKtCppRdCall by creating(InteropTask::class) {
        taskServer = CrossTestKtServerRdCall
        taskClient = getCppTaskByName("CrossTestCppClientRdCall")

        addDependencies()
    }
//endregion

//region KtCs
    val CrossTestKtCsAllEntities by creating(InteropTask::class) {
        taskServer = CrossTestKtServerAllEntities
        taskClient = getCsTaskByName("CrossTestCsClientAllEntities")

        addDependencies()
    }

    val CrossTestKtCsBigBuffer by creating(InteropTask::class) {
        taskServer = CrossTestKtServerBigBuffer
        taskClient = getCsTaskByName("CrossTestCsClientBigBuffer")

        addDependencies()
    }

    val CrossTestKtCsRdCall by creating(InteropTask::class) {
        taskServer = CrossTestKtServerRdCall
        taskClient = getCsTaskByName("CrossTestCsClientRdCall")

        addDependencies()
    }
//endregion

    val interopTasks = listOf(CrossTestKtCppAllEntities, CrossTestKtCppBigBuffer, CrossTestKtCppRdCall,
            CrossTestKtCsAllEntities, CrossTestKtCsBigBuffer, CrossTestKtCsRdCall)
}*/
