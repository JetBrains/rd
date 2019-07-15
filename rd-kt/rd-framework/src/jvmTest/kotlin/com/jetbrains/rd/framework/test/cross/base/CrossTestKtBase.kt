package com.jetbrains.rd.framework.test.cross.base

import com.jetbrains.rd.framework.IProtocol
import com.jetbrains.rd.framework.base.RdReactiveBase
import com.jetbrains.rd.util.ConsoleLoggerFactory
import com.jetbrains.rd.util.ILoggerFactory
import com.jetbrains.rd.util.Statics
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.reactive.ISource
import com.jetbrains.rd.util.spinUntil
import com.jetbrains.rd.util.string.PrettyPrinter
import com.jetbrains.rd.util.string.println
import java.io.File

abstract class CrossTestKtBase {
    protected val testName: String = this.javaClass.kotlin.simpleName!!

    protected val printer = PrettyPrinter()
    protected lateinit var outputFile: File

    @Volatile
    protected var finished = false

    protected lateinit var scheduler: IScheduler
    protected lateinit var protocol: IProtocol

    private val modelLifetimeDef = Lifetime.Eternal.createNested()
    private val socketLifetimeDef = Lifetime.Eternal.createNested()

    protected val modelLifetime = modelLifetimeDef.lifetime
    protected val socketLifetime = socketLifetimeDef.lifetime
    protected val <T> ISource<T>.isLocalChange
        get() = (this as? RdReactiveBase)?.isLocalChange == true

    protected fun before(args: Array<String>) {
        if (args.size != 1) {
            throw IllegalArgumentException("Wrong number of arguments for $testName:${args.size}")
        }
        val outputFileName = args[0]
        outputFile = File(outputFileName)
        println("Test:$testName started, file=$outputFileName")
    }

    protected fun after() {
        spinUntil(10_000) { finished }
        spinUntil(1_000) { false }

        socketLifetimeDef.terminate()
        modelLifetimeDef.terminate()
        outputFile.parentFile.mkdirs()
        outputFile.printWriter().use { writer ->
            writer.println(printer)
        }
    }

    protected fun <T> PrettyPrinter.printIfRemoteChange(entity: ISource<T>, entityName: String, vararg values: Any) {
        if (!entity.isLocalChange) {
            printAnyway(entityName, values)
        }
    }

    protected fun PrettyPrinter.printAnyway(entityName: String, vararg values: Any) {
        println("***")
        println("$entityName:")
        values.forEach { value -> value.println(this) }
    }

    fun run(args: Array<String>) {
        Statics<ILoggerFactory>().push(ConsoleLoggerFactory).use {
            start(args)
        }
    }

    abstract fun start(args: Array<String>);
}