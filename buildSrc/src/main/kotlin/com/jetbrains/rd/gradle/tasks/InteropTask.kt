package com.jetbrains.rd.gradle.tasks

import com.jetbrains.rd.gradle.tasks.util.portFile
import com.jetbrains.rd.gradle.tasks.util.portFileClosed
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.time.LocalTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

const val PROCESS_WAIT_TIMEOUT = 20L

@Suppress("UnstableApiUsage")
open class InteropTask : DefaultTask() {
//    @Input
    lateinit var taskServer: MarkedExecTask
//    @Input
    lateinit var taskClient: MarkedExecTask

    private val serverRunningCommand by lazy { taskServer.commandLineWithArgs }
    private val clientRunningCommand by lazy { taskClient.commandLineWithArgs }

    private val processes: MutableList<ProcessBuilder> = mutableListOf()

    init {
        group = "interop"
        outputs.upToDateWhen { false }
    }

    fun lateInit() {
        dependsOn((taskServer as Task).taskDependencies)
        dependsOn((taskClient as Task).taskDependencies)
    }

    private fun executeTask(task: MarkedExecTask, command: List<String>) {
        println("Interop task: async running task=$task, " +
            "command=${command}, " +
            "working dir=${task.getWorkingDir()}")
        try {
            val outputFile = createTempFile(suffix = ".txt")
            val errorFile = createTempFile(suffix = ".txt")
            println("outputFile=${outputFile}, errorFile=${errorFile}")
            val process = ProcessBuilder(command).apply {
                directory(task.getWorkingDir())
            }
                .redirectOutput(outputFile)
                .redirectError(errorFile)
            processes.add(process)
        } catch (e: Exception) {
            println("Error occurred while building process by async task=$task, ${e.stackTrace}")
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
        processes.forEach { pb ->
            thread {
                try {
                    val p = pb.start()
                    val exitStatus = p.waitFor(PROCESS_WAIT_TIMEOUT, TimeUnit.SECONDS)
                    if (!exitStatus) {
                        println("$p exit with status=$exitStatus")
                    }
                    p.destroyForcibly()
                } catch (e: Exception) {
                    println("Error occurred while process executing:")
                    e.printStackTrace()
                } finally {
                    countDownLatch.countDown()
                }
            }
        }
        countDownLatch.await()

        println("At ${LocalTime.now()}: countDownLatch awaited")
    }
}