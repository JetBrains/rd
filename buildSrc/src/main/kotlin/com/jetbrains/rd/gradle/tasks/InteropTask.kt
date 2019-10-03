package com.jetbrains.rd.gradle.tasks

import com.jetbrains.rd.gradle.tasks.util.portFile
import com.jetbrains.rd.gradle.tasks.util.portFileClosed
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.nio.file.Paths
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread


@Suppress("UnstableApiUsage")
open class InteropTask : DefaultTask() {
    @Input
    lateinit var taskServer: MarkedExecTask
    @Input
    lateinit var taskClient: MarkedExecTask

    private val serverRunningCommand by lazy { taskServer.commandLineWithArgs }
    private val clientRunningCommand by lazy { taskClient.commandLineWithArgs }

    private val processes: MutableList<Process> = mutableListOf()

    init {
        group = "interop"
    }

    fun lateInit() {
        dependsOn((taskServer as Task).taskDependencies)
        dependsOn((taskClient as Task).taskDependencies)
    }

    private fun executeTask(task: MarkedExecTask, command: String) {
        println("Interop task: async running task=$task, " +
                "command=${command}, " +
                "working dir=${task.getWorkingDir()}")
        try {
            val process = Runtime.getRuntime().exec(command, null, task.getWorkingDir()) //only JVM
            processes.add(process)
        } catch (e: Exception) {
            println("Error occurred while starting async $task, ${e.stackTrace}")
        } finally {
        }
    }

    private fun runServer() {
        executeTask(taskServer, serverRunningCommand)
    }

    private fun startClient() {
        executeTask(taskClient, clientRunningCommand)
    }

    private fun beforeStart() {
        assert(portFile.delete())
        assert(portFileClosed.delete())
        System.setProperty("TmpSubDirectory", name)
        assert((taskServer as Task).tmpFileDirectory.deleteRecursively())
        assert((taskServer as Task).tmpFileDirectory.mkdirs())
    }

    @TaskAction
    internal fun start() {
        beforeStart()

        runServer()
        startClient()

        val countDownLatch = CountDownLatch(2)
        processes.forEach { p ->
            thread {
                val exitStatus = p.waitFor(20, TimeUnit.SECONDS)
                if (!exitStatus) {
                    println("$p exit with status=$exitStatus")
                }
                p.destroyForcibly()
                countDownLatch.countDown()
            }
        }
        countDownLatch.await()

        processes.forEach { p ->
            println("$p error stream:")
            p.errorStream.bufferedReader().forEachLine { line ->
                println(line)
            }

            println("$p output stream:")
            p.inputStream.bufferedReader().forEachLine { line ->
                println(line)
            }
        }
    }
}