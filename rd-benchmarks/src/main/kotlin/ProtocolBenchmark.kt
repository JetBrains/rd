package com.jetbrains.rd.benchmarks

import com.jetbrains.rd.benchmarks.model.BenchmarkRootModel
import com.jetbrains.rd.benchmarks.model.InterningBenchmarkRoot
import com.jetbrains.rider.framework.*
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.lifetime.LifetimeDefinition
import com.jetbrains.rider.util.reactive.*
import com.jetbrains.rider.util.threading.SingleThreadScheduler
import java.util.concurrent.CountDownLatch
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

data class PerfReport(val name: String, val bytes: Long, val timeNs: Long, val iterations: Int)

class ProtocolSide(wireCallback: (Lifetime, IScheduler) -> SocketWire.Base) {
    val lifetime = LifetimeDefinition()
    private val serializers = Serializers()
    private val identities = Identities(IdKind.Server)
    private val scheduler = /*BenchmarkScheduler()*/ SingleThreadScheduler(lifetime.lifetime, "Wirey Scheduler")
    val wire = wireCallback(lifetime.lifetime, scheduler)
    private val protocol = Protocol(serializers, identities, scheduler, wire)

    val model = run {
        var model: BenchmarkRootModel? = null
        wire.scheduler.invokeOrQueue {
            model = BenchmarkRootModel.create(lifetime, protocol)
        }
        while(model == null) Thread.sleep(1)
        model!!
    }
}

@ExperimentalContracts
inline fun withProtocolPair(block: (ProtocolSide, ProtocolSide) -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    val server = ProtocolSide { lifetime, scheduler -> SocketWire.Server(lifetime, scheduler, port = null, optId = "Server") }
    val client = ProtocolSide { lifetime, scheduler -> SocketWire.Client(lifetime, scheduler, port = server.wire.serverPort, optId = "Client") }

    while(!server.wire.connected.value) Thread.sleep(1)

    server.wire.scheduler.invokeOrQueue {
        server.model.interningRoot.set(InterningBenchmarkRoot())
    }
    while (!client.model.interningRoot.hasValue) Thread.sleep(1)

    try {
        block(server, client)
    } finally {
        client.lifetime.terminate()
        server.lifetime.terminate()
    }
}

inline fun iterateForTime(minIterations: Int, minTimeMs: Int, maxTimeMs: Int = -1, block: (Int) -> Unit): Int {
    var i = 0
    val startTime = System.nanoTime()
    while(true) {
        block(i)

        val timePassed = System.nanoTime() - startTime
        if(i > minIterations && timePassed > minTimeMs * 1_000_000L || maxTimeMs > 0 && timePassed > maxTimeMs * 1_000_000L)
            break
        i++
    }
    return i
}

inline fun <T> benchmarkSignalWrites(benchmarkName: String, minIterations: Int, minTimeMs: Int, maxTimeMs: Int, signalGetter: (BenchmarkRootModel) -> ISignal<T>, endValue: T, crossinline valueSource: (Int) -> T):PerfReport {
    @UseExperimental(ExperimentalContracts::class)
    withProtocolPair { server, client ->
        val serverSignal = signalGetter(server.model)
        val clientSignal = signalGetter(client.model)

        val latch = CountDownLatch(1)
        var startTime: Long = 0
        var iterations = -1
        Lifetime.using { lt ->
            val clientAdviseLatch = CountDownLatch(1)
            client.wire.scheduler.invokeOrQueue {
                clientSignal.advise(lt) { if(it == endValue) latch.countDown() }
                clientAdviseLatch.countDown()
            }

            clientAdviseLatch.await()

            startTime = System.nanoTime()
            server.wire.scheduler.invokeOrQueue {
                iterations = iterateForTime(minIterations, minTimeMs, maxTimeMs) { i ->
                    serverSignal.fire(valueSource(i))
                }
                serverSignal.fire(endValue)
            }

            latch.await()
        }
        val endTime = System.nanoTime()
        return@benchmarkSignalWrites PerfReport(benchmarkName, server.wire.bytesWritten, endTime - startTime, iterations)
    }
//    error("unreachable")
}

fun PerfReport.statsString(header: String = name) = "$header in ${timeNs/1000000f} ms, speed ${bytes/1024.0/1024.0/(timeNs/1_000_000_000f)} MB/s, ${iterations/(timeNs/1000000000f)} it/s"

fun getPrecalculatedStringProvider(stringLength: Int, uniqueValues: Int): (Int) -> String {
    val base = "a".repeat(stringLength)
    val values = (0 until uniqueValues).map { base + it }
    return { values[it % uniqueValues] }
}


fun getNonPrecalculatedStringProvider(stringLength: Int): (Int) -> String {
    val base = "a".repeat(stringLength)
    return { base + it }
}

val intSignalAtRoot: (BenchmarkRootModel) -> ISignal<Int> = { it.count }
val intSignalInner: (BenchmarkRootModel) -> ISignal<Int> = { it.interningRoot.valueOrThrow.notInternedInt }
val stringSignalNoIntern: (BenchmarkRootModel) -> ISignal<String> = { it.interningRoot.valueOrThrow.notInternedString }
val stringSignalLocalIntern: (BenchmarkRootModel) -> ISignal<String> = { it.interningRoot.valueOrThrow.internedHereString }
val stringSignalMissingIntern: (BenchmarkRootModel) -> ISignal<String> = { it.interningRoot.valueOrThrow.internedInMissingRootString }
val stringSignalProtocolIntern: (BenchmarkRootModel) -> ISignal<String> = { it.interningRoot.valueOrThrow.internedInProtocolString }

fun benchmarkSuite(warmupRun: Boolean): java.lang.StringBuilder {
    val baseIterations = if(warmupRun) 1_000_000 else 100_000
    val baseTime = if(warmupRun) 1_000 else 10_000
    val maxTime = if(warmupRun) 1_000 else -1

    val builder = StringBuilder()

    val intSignalProviders = listOf("root" to intSignalAtRoot, "inner" to intSignalInner)
    val stringSignalProviders = listOf(
            "not interned" to stringSignalNoIntern, "interned locally" to stringSignalLocalIntern,
            "missed intern" to stringSignalMissingIntern, "protocol intern" to stringSignalProtocolIntern
    )

    val stringValueProviders = listOf(
            "raw sl=1" to getNonPrecalculatedStringProvider(1),
            "pre sl=1, uq=100" to getPrecalculatedStringProvider(1, 100),
            "pre sl=1, uq=10_000" to getPrecalculatedStringProvider(1, 10_000),

            "raw sl=100" to getNonPrecalculatedStringProvider(100),
            "pre sl=100, uq=100" to getPrecalculatedStringProvider(100, 100),
            "pre sl=100, uq=10_000" to getPrecalculatedStringProvider(100, 10_000),

            "raw sl=10_000" to getNonPrecalculatedStringProvider(10_000),
            "pre sl=10_000, uq=100" to getPrecalculatedStringProvider(10_000, 100)
    )

    intSignalProviders.forEach {
        val benchmarkName = "int ${it.first}"
        println("run $benchmarkName")
        builder.appendln(benchmarkSignalWrites(benchmarkName, baseIterations, baseTime, maxTime, it.second, -1, { i -> i }).statsString())
    }

    builder.appendln()

    stringValueProviders.forEach { values ->
        builder.appendln()
        stringSignalProviders.forEach { signal ->
            val benchmarkName = "string ${signal.first} ${values.first}"
            println("run $benchmarkName")
            builder.appendln(benchmarkSignalWrites(benchmarkName, baseIterations, baseTime, maxTime, signal.second, "", values.second).statsString())
        }
    }

    return builder
}

fun main() {
    val origSuite = benchmarkSuite(true)
    benchmarkSuite(true)
    benchmarkSuite(true)

    val benchRun = benchmarkSuite(false)

    println("First warmup run")
    println(origSuite)
    println("\n\n\nActual run")
    println(benchRun)
}
