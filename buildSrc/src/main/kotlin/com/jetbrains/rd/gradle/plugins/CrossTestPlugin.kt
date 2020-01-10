@file:Suppress("LocalVariableName")

package com.jetbrains.rd.gradle.plugins

import com.jetbrains.rd.gradle.tasks.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.*

fun Project.applyCrossTest() = apply<CrossTestPlugin>()

@Suppress("UNUSED_VARIABLE")
class CrossTestPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = target.run {
        applyKotlinJVM()

        val `rd-net` = rootProject.project(":rd-net")
//        val `rd-cpp` = rootProject.project(":rd-cpp")
        val `rd-framework` = project(":rd-framework")

        evaluationDependsOn(`rd-net`.path)
//        evaluationDependsOn(`rd-cpp`.path)

        dependencies {
            "implementation"(`rd-framework`)
            "implementation"(gradleApi())
        }

//        fun getCppTaskByName(name: String): CrossTestCppTask {
//            return `rd-cpp`.tasks.getByName<CrossTestCppTask>(name)
//        }

        fun getCsTaskByName(name: String): CrossTestCsTask {
            return `rd-net`.tasks.getByName<CrossTestCsTask>(name)
        }

        tasks {
            //region Kt

            fun CrossTestTaskKt.initialize() {
                dependsOn("classes")
                dependsOn(":rd-net:buildCrossTests")
                classpath += files(target.buildDir
                        .resolve("classes")
                        .resolve("kotlin")
                        .resolve("main"))
                classpath += sourceSets["main"].output
                classpath += configurations["compileClasspath"]
                classpath += configurations["runtimeClasspath"]
            }

            fun creatingCrossTestRdTask() =
                creating(CrossTestTaskKt::class) {
                    initialize()
                }

            val CrossTest_AllEntities_KtServer by creatingCrossTestRdTask()

            val CrossTest_AllEntities_KtClient by creatingCrossTestRdTask()

            val CrossTest_BigBuffer_KtServer by creatingCrossTestRdTask()

            val CrossTest_RdCall_KtServer by creatingCrossTestRdTask()
//endregion

//region KtCpp
            /*val CrossTest_AllEntities_KtServer_CppClient by creating(InteropTask::class) {
                taskServer = CrossTest_AllEntities_KtServer
                taskClient = getCppTaskByName("CrossTest_AllEntities_CppClient")

                lateInit()
            }

            val CrossTest_BigBuffer_KtServer_CppClient by creating(InteropTask::class) {
                taskServer = CrossTest_BigBuffer_KtServer
                taskClient = getCppTaskByName("CrossTest_BigBuffer_CppClient")

                lateInit()
            }

            val CrossTest_RdCall_KtServer_CppClient by creating(InteropTask::class) {
                taskServer = CrossTest_RdCall_KtServer
                taskClient = getCppTaskByName("CrossTest_RdCall_CppClient")

                lateInit()
            }*/
//endregion

//region KtCs
            val CrossTest_AllEntities_KtServer_CsClient by creating(InteropTask::class) {
                taskServer = CrossTest_AllEntities_KtServer
                taskClient = getCsTaskByName("CrossTest_AllEntities_CsClient")

                lateInit()
            }

            val CrossTest_AllEntities_CsServer_KtClient by creating(InteropTask::class) {
                taskServer = getCsTaskByName("CrossTest_AllEntities_CsServer")
                taskClient = CrossTest_AllEntities_KtClient

                lateInit()
            }

            val CrossTest_BigBuffer_KtServer_CsClient by creating(InteropTask::class) {
                taskServer = CrossTest_BigBuffer_KtServer
                taskClient = getCsTaskByName("CrossTest_BigBuffer_CsClient")

                lateInit()
            }

            val CrossTest_RdCall_KtServer_CsClient by creating(InteropTask::class) {
                taskServer = CrossTest_RdCall_KtServer
                taskClient = getCsTaskByName("CrossTest_RdCall_CsClient")

                lateInit()
            }
//endregion

            withType(Test::class) {
                dependsOn(withType(InteropTask::class))
            }
        }
    }
}