package com.jetbrains.rd.cross.base

import com.jetbrains.rd.cross.util.CrossTestsLoggerFactory
import com.jetbrains.rd.cross.util.logWithTime
import com.jetbrains.rd.framework.IProtocol
import com.jetbrains.rd.framework.base.RdReactiveBase
import com.jetbrains.rd.util.*
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.reactive.ISource
import com.jetbrains.rd.util.string.PrettyPrinter
import com.jetbrains.rd.util.string.println
import java.io.File
import kotlin.io.use

private const val SPINNING_TIMEOUT = 10_000L

abstract class CrossTestKtBase {
    private val testName: String = this.javaClass.kotlin.simpleName!!

    protected val printer = PrettyPrinter()
    private lateinit var outputFile: File

    protected lateinit var scheduler: IScheduler
    protected lateinit var protocol: IProtocol

    private val modelLifetimeDef = Lifetime.Eternal.createNested()
    private val socketLifetimeDef = Lifetime.Eternal.createNested()

    protected val modelLifetime = modelLifetimeDef.lifetime
    protected val socketLifetime = socketLifetimeDef.lifetime
    protected val <T> ISource<T>.isLocalChange
        get() = (this as? RdReactiveBase)?.isLocalChange == true

    protected fun before(args: Array<String>) {
        check(args.size == 1) {
            "Wrong number of arguments for $testName:${args.size}, expected 1. main([\"CrossTestKtServerAllEntities\"]) for example."
        }
        val outputFileName = args[0]
        outputFile = File(outputFileName)
        println("Test:$testName started, file=$outputFileName")
    }

    protected fun after() {
        logWithTime("Spinning started")
        spinUntil(SPINNING_TIMEOUT) { false }
        logWithTime("Spinning finished")

        socketLifetimeDef.terminate()
        modelLifetimeDef.terminate()
        outputFile.parentFile.mkdirs()
        outputFile.printWriter().use { writer ->
            writer.println(printer)
        }
    }

    protected fun <T> PrettyPrinter.printIfRemoteChange(entity: ISource<T>, entityName: String, vararg values: Any) {
        if (!entity.isLocalChange) {
            printAnyway(entityName, *values)
        }
    }

    protected fun PrettyPrinter.printAnyway(entityName: String, vararg values: Any) {
        println("***")
        println("$entityName:")
        values.forEach { value -> value.println(this) }
    }

    fun run(args: Array<String>) {
        logWithTime("Test run")

        Statics<ILoggerFactory>().apply {
            use(ConsoleLoggerFactory.apply {
                minLevelToLog = LogLevel.Trace
            }) {
                val factory = CrossTestsLoggerFactory()
                use(factory) {
                    try {
                        start(args)
                    } catch (e: Throwable) {
                        e.printStackTrace()
                        throw e
                    } finally {
                        if (::outputFile.isInitialized) {
                            outputFile.writeText(factory.toString())
                        }
                    }
                }
            }
        }
    }

    abstract fun start(args: Array<String>)
}