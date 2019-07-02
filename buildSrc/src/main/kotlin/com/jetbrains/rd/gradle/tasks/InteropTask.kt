package com.jetbrains.rd.gradle.tasks

import com.jetbrains.rd.gradle.tasks.util.portFile
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Task
import org.gradle.api.tasks.StopActionException
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import java.io.File
import java.nio.file.Files
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


@Suppress("UnstableApiUsage")
open class InteropTask : DefaultTask() {
    var workerExecutor: ExecutorService = Executors.newFixedThreadPool(2)
    lateinit var taskServer: Task
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

        compareWithGold()
    }

    private fun contentEquals(file1: File, file2: File): Boolean {
        val f1 = Files.readAllBytes(file1.toPath())
        val f2 = Files.readAllBytes(file2.toPath())
        return Arrays.equals(f1, f2)
    }

    private fun assertEqualsFiles(file1: String, file2: String) {
        if (contentEquals(File(file1), File(file2))) {
            println("The files $file1 and $file2 are same!")
        } else {
            throw StopActionException("The files $file1 and $file2 differ!")
        }
    }

    private fun compareWithGold() {
        assertEqualsFiles(taskServer.goldFilePath, taskServer.tmpFilePath)
        assertEqualsFiles(taskClient.goldFilePath, taskClient.tmpFilePath)
    }
}