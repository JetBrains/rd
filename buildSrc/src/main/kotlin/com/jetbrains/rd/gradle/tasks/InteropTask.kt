package com.jetbrains.rd.gradle.tasks

import com.jetbrains.rd.gradle.tasks.util.portFile
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.tasks.TaskAction
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


@Suppress("UnstableApiUsage")
open class InteropTask : DefaultTask() {
    var workerExecutor: ExecutorService = Executors.newFixedThreadPool(2)
    lateinit var taskServer: Task
    lateinit var taskClient: Task

    private fun executeTask(task: Task) {
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

    @TaskAction
    internal fun start() {
        assert(portFile.delete())
        runServer()
        startClient()

        workerExecutor.shutdown()
        workerExecutor.awaitTermination(20, TimeUnit.SECONDS)
    }
}