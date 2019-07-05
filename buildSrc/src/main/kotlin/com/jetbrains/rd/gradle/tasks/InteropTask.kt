package com.jetbrains.rd.gradle.tasks

import com.jetbrains.rd.gradle.tasks.util.portFile
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject


@Suppress("UnstableApiUsage")
open class InteropTask : DefaultTask() {
    private val workerExecutor: ExecutorService = Executors.newFixedThreadPool(2)

    @Input
    lateinit var taskServer: Task

    @Input
    lateinit var taskClient: Task

    fun addDependencies() {
        dependsOn(taskServer.taskDependencies)
        dependsOn(taskClient.taskDependencies)
    }

    /*init {
        println("${taskServer.dependsOn + taskClient.dependsOn}")
//        println("init getDependsOn:${super.getDependsOn() + taskServer.dependsOn + taskClient.dependsOn}")
        *//*(taskServer.dependsOn + taskClient.dependsOn).forEach {
            this.dependsOn(it)
        }*//*
    }

    override fun getDependsOn(): Set<Any> {
        println("getDependsOn:${super.getDependsOn() + taskServer.dependsOn + taskClient.dependsOn}")
        return super.getDependsOn() + taskServer.dependsOn + taskClient.dependsOn
    }*/

    private fun executeTask(task: Task) {
        println("executeTask")
        (task.actions).first().let {
            workerExecutor.submit {
                it.execute(task)
            }
        }
    }

    private fun runServer() {
        executeTask(taskServer)
    }

    private fun startClient() {
        executeTask(taskClient)
    }

    private fun beforeStart() {
        assert(portFile.delete())
        assert(File(taskServer.tmpFilePath).delete())
        assert(File(taskClient.tmpFilePath).delete())
    }

    @TaskAction
    internal fun start() {
        beforeStart()

        runServer()
        startClient()

        workerExecutor.shutdown()
        workerExecutor.awaitTermination(200, TimeUnit.SECONDS)
    }
}