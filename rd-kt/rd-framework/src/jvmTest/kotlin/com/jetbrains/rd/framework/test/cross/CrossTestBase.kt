package com.jetbrains.rd.framework.test.cross

import com.jetbrains.rd.framework.IProtocol
import com.jetbrains.rd.framework.base.RdReactiveBase
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.reactive.ISource
import com.jetbrains.rd.util.spinUntil
import com.jetbrains.rd.util.string.PrettyPrinter
import com.jetbrains.rd.util.string.println
import java.io.File

abstract class CrossTestBase {
    protected abstract val testName: String

    protected val printer = PrettyPrinter()
    protected lateinit var outputFile: File

    @Volatile
    protected var finished = false

    protected lateinit var scheduler: IScheduler
    protected lateinit var protocol: IProtocol

    private val lifetimeDef = Lifetime.Eternal.createNested()
    private val socketLifetimeDef = Lifetime.Eternal.createNested()

    protected val lifetime = lifetimeDef.lifetime
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
        lifetimeDef.terminate()
        outputFile.parentFile.mkdirs()
        outputFile.printWriter().use { writer ->
            writer.println(printer)
        }
    }

    protected fun <T> PrettyPrinter.printIfRemoteChange(entity: ISource<T>, entityName: String, vararg values: Any) {
        if (!entity.isLocalChange) {
            println("***")
            println("$entityName:")
            values.forEach { value -> value.println(this) }
        }
    }

    abstract fun run(args: Array<String>)
}